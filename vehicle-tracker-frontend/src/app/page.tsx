'use client';

import { useEffect, useState } from 'react';
import { vehicleApi, VehicleStatus } from './lib/api';
import KakaoMap from './components/KakaoMap';

interface DeviceTrackingInfo {
  deviceId: string;
  deviceName: string;
  isOnline: boolean;
  lastEngineStatus: string;
  lastSpeed: number;
  lastLocation?: {
    latitude: number;
    longitude: number;
  };
  lastUpdate: string;
  connectionTime?: string;
  totalUpdates: number;
}

export default function Home() {
  const [currentStatus, setCurrentStatus] = useState<VehicleStatus | null>(null);
  const [statusHistory, setStatusHistory] = useState<VehicleStatus[]>([]);
  const [deviceTracking, setDeviceTracking] = useState<Map<string, DeviceTrackingInfo>>(new Map());
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);

  // 로컬 타임스탬프 파싱 (KST 기준)
  const parseLocalTimestamp = (timestamp: string): number => {
    try {
      // 이미 "YYYY-MM-DD HH:mm:ss" 형식이면 수동 파싱
      if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(timestamp)) {
        const [datePart, timePart] = timestamp.split(' ');
        const [year, month, day] = datePart.split('-').map(Number);
        const [hours, minutes, seconds] = timePart.split(':').map(Number);
        return new Date(year, month - 1, day, hours, minutes, seconds).getTime();
      }
      // ISO-8601 등은 기본 파서 사용
      const t = Date.parse(timestamp);
      return isNaN(t) ? 0 : t;
    } catch {
      return 0;
    }
  };

  // 백엔드 연결 상태 확인
  const checkConnection = async () => {
    const connected = await vehicleApi.testConnection();
    setIsConnected(connected);
    return connected;
  };

  // 현재 상태 조회
  const fetchCurrentStatus = async () => {
    const status = await vehicleApi.getCurrentStatus();
    console.log('Received status:', status); // 디버깅용 - 전체 상태 확인
    console.log('Timestamp format:', status?.timestamp); // timestamp 형식 확인
    
    // 상태가 있고 유효한 디바이스면 상태 업데이트
    if (status) {
      // 엔진 상태가 OFF면 현재 상태에서 숨김
      if (status.engineStatus === 'OFF') {
        setCurrentStatus(null);
      } else {
        // 엔진 상태가 ON일 때만 표시
        setCurrentStatus(status);
      }
    } else {
      setCurrentStatus(null);
    }
  };

  // 상태 이력 조회 및 디바이스 추적 정보 업데이트
    const fetchStatusHistory = async () => {
      const history = await vehicleApi.getStatusHistory();
      console.log('Received history:', history); // 디버깅용 - 전체 히스토리 확인
      
      // 더 정확한 필터링 적용: Unknown Device이면서 OFF 상태인 것만 제외
      const filteredHistory = history.filter(status => {
        // Unknown Device이면서 OFF 상태인 것만 제외
        if (status.deviceName === 'Unknown Device' && status.engineStatus === 'OFF') {
          return false;
        }
        return true;
      });
      
      if (filteredHistory.length > 0) {
        console.log('First history timestamp:', filteredHistory[0].timestamp); // 형식 확인
      }
      
      setStatusHistory(filteredHistory);
      updateDeviceTracking(filteredHistory);
    };

  // 디바이스별 추적 정보 업데이트 함수
  const updateDeviceTracking = (history: VehicleStatus[]) => {
    const newDeviceTracking = new Map<string, DeviceTrackingInfo>();
    
    // 역순으로 정렬하여 최신 데이터를 우선 처리
    const sortedHistory = [...history].sort((a, b) => {
      const tb = parseLocalTimestamp(b.timestamp);
      const ta = parseLocalTimestamp(a.timestamp);
      if (tb !== ta) return tb - ta;
      // 동시간대(tie)일 경우 같은 디바이스에서는 OFF를 ON보다 우선하도록 함
      if (a.deviceId === b.deviceId && a.deviceName === b.deviceName) {
        if (a.engineStatus === b.engineStatus) return 0;
        return a.engineStatus === 'OFF' ? -1 : 1;
      }
      // 다른 디바이스 간에는 상대 순서를 유지
      return 0;
    });
    
    for (const status of sortedHistory) {
      const deviceKey = `${status.deviceId}-${status.deviceName}`;
      
      if (!newDeviceTracking.has(deviceKey)) {
        // 새로운 디바이스 추가
        const isOnline = status.engineStatus === 'ON';
        const deviceInfo: DeviceTrackingInfo = {
          deviceId: status.deviceId,
          deviceName: status.deviceName,
          isOnline: isOnline,
          lastEngineStatus: status.engineStatus,
          lastSpeed: status.speed,
          lastLocation: status.location,
          lastUpdate: status.timestamp,
          totalUpdates: 1
        };
        
        // 연결 시작 시간 찾기 (가장 오래된 ON 상태)
        const connectionStart = history
          .filter(h => h.deviceId === status.deviceId && 
                      h.deviceName === status.deviceName && 
                      h.engineStatus === 'ON')
          .sort((a, b) => parseLocalTimestamp(a.timestamp) - parseLocalTimestamp(b.timestamp))[0];
        
        if (connectionStart) {
          deviceInfo.connectionTime = connectionStart.timestamp;
        }
        
        newDeviceTracking.set(deviceKey, deviceInfo);
      } else {
        // 기존 디바이스 업데이트 (최신 상태 반영)
        const existing = newDeviceTracking.get(deviceKey)!;
        existing.totalUpdates++;
        
        // 최신 상태 정보 업데이트
        const statusTs = parseLocalTimestamp(status.timestamp);
        const existingTs = parseLocalTimestamp(existing.lastUpdate);
        const shouldUpdate =
          statusTs > existingTs ||
          (statusTs === existingTs && status.engineStatus === 'OFF' && existing.lastEngineStatus !== 'OFF');

        if (shouldUpdate) {
          existing.lastEngineStatus = status.engineStatus;
          existing.isOnline = status.engineStatus === 'ON';
          existing.lastSpeed = status.speed;
          existing.lastUpdate = status.timestamp;
          
          // 위치 정보가 있으면 업데이트
          if (status.location) {
            existing.lastLocation = status.location;
          }
        }
        
        // 최신 기록에 위치가 없을 수 있으므로, 과거 기록 중 가장 최근 위치로 보완
        if (!existing.lastLocation && status.location) {
          existing.lastLocation = status.location;
        }
      }
    }
    
    setDeviceTracking(newDeviceTracking);
  };

  // 자동 새로고침 (5초마다)
  useEffect(() => {
    const interval = setInterval(() => {
      if (isConnected) {
        fetchCurrentStatus();
        fetchStatusHistory();
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [isConnected]);

  // 초기 데이터 로드
  useEffect(() => {
    const initializeData = async () => {
      setLoading(true);
      await checkConnection();
      await fetchCurrentStatus();
      await fetchStatusHistory();
      setLoading(false);
    };

    initializeData();
  }, []);

  // 시간 포맷팅 함수 (KST 시간 문자열을 올바르게 처리)
  const formatTimestamp = (timestamp: string) => {
    try {
      // 이미 "YYYY-MM-DD HH:mm:ss" 형식이면 그대로 반환 (백엔드에서 KST로 보낸 것)
      if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(timestamp)) {
        return timestamp; // KST 시간을 그대로 표시
      }
      
      // ISO-8601 형식이면 KST로 변환
      if (timestamp.includes('T') || timestamp.includes('Z') || timestamp.includes('+')) {
        const date = new Date(timestamp);
        if (isNaN(date.getTime())) {
          return timestamp; // 파싱 실패 시 원본 반환
        }
        
        // KST로 변환하여 "YYYY-MM-DD HH:mm:ss" 형식으로 표시
        const kstDate = new Date(date.getTime() + (9 * 60 * 60 * 1000)); // UTC + 9시간
        const year = kstDate.getUTCFullYear();
        const month = String(kstDate.getUTCMonth() + 1).padStart(2, '0');
        const day = String(kstDate.getUTCDate()).padStart(2, '0');
        const hours = String(kstDate.getUTCHours()).padStart(2, '0');
        const minutes = String(kstDate.getUTCMinutes()).padStart(2, '0');
        const seconds = String(kstDate.getUTCSeconds()).padStart(2, '0');
        
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      }
      
      // 기타 형식은 그대로 반환
      return timestamp;
    } catch (error) {
      console.error('Timestamp formatting error:', error);
      return timestamp; // 에러 시 원본 반환
    }
  };

  // 시간만 추출해주는 함수.
  const formatTimeOnly = (timestamp: string) => {
    const timePart = timestamp.split(' ')[1];
    return timePart || timestamp;
  };

  // 연결 지속 시간 계산
  const calculateDuration = (connectionTime: string, lastUpdate: string) => {
    try {

        const parseLocalTime = (timeStr: string) => {
            const [datePart, timePart] = timeStr.split(' ');
            const [year, month, day] = datePart.split('-').map(Number);
            const [hours, minutes, seconds] = timePart.split(':').map(Number);
            return new Date(year, month - 1, day, hours, minutes, seconds).getTime();
        };
        
        const start = parseLocalTime(connectionTime);
        const end = parseLocalTime(lastUpdate);
        const durationMs = end - start;
        
        const minutes = Math.floor(durationMs / 60000);
        const seconds = Math.floor((durationMs % 60000) / 1000);
        
        if (minutes > 0) {
            return `${minutes}분 ${seconds}초`;
        } else {
            return `${seconds}초`;
        }
    } catch (error) {
        return '계산 불가';
    }
  };

  if (loading) {
    return (
      <main className="container mx-auto p-4">
        <div className="text-center">로딩 중...</div>
      </main>
    );
  }

  return (
    <main className="container mx-auto p-4 space-y-6">
      <h1 className="text-3xl font-bold text-center">차량 실시간 모니터링 시스템</h1>
      
      <div className="bg-gray-100 p-4 rounded-lg">
        <h2 className="text-xl font-semibold mb-2">시스템 상태</h2>
        <div className="flex items-center space-x-4">
          <div className={`inline-block px-3 py-1 rounded-full text-white ${
            isConnected ? 'bg-green-500' : 'bg-red-500'
          }`}>
            {isConnected ? '서버 연결됨' : '서버 연결 실패'}
          </div>
          <button 
            onClick={checkConnection}
            className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          >
            연결 확인
          </button>
          <div className="text-sm text-gray-600">
            자동 새로고침: 3초마다
          </div>
        </div>
      </div>

      <div className="bg-white border border-gray-200 p-4 rounded-lg shadow-sm">
        <h2 className="text-xl font-semibold mb-4 flex items-center">
          🚗 현재 차량 상태
          {currentStatus && (
            <span className="ml-2 text-sm text-green-600">● LIVE</span>
          )}
        </h2>
        
        {currentStatus ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <p><strong>기기 ID:</strong> {currentStatus.deviceId}</p>
              <p><strong>블루투스 기기:</strong> {currentStatus.deviceName}</p>
              <p><strong>시동 상태:</strong> 
                <span className={`ml-2 px-2 py-1 rounded text-white text-sm ${
                  currentStatus.engineStatus === 'ON' ? 'bg-green-500' : 'bg-red-500'
                }`}>
                  {currentStatus.engineStatus}
                </span>
              </p>
              <p><strong>속도:</strong> 
                <span className="ml-2 text-lg font-mono">
                  {currentStatus.speed} km/h
                </span>
              </p>
              <p><strong>마지막 업데이트:</strong> 
                <span className="ml-2 text-sm">
                  {formatTimestamp(currentStatus.timestamp)}
                </span>
              </p>
            </div>
            
            <div className="space-y-2">
              {currentStatus.location ? (
                <>
                  <p><strong>📍 현재 위치:</strong></p>
                  <div className="bg-gray-50 p-3 rounded text-sm font-mono">
                    <p>위도: {currentStatus.location.latitude.toFixed(6)}</p>
                    <p>경도: {currentStatus.location.longitude.toFixed(6)}</p>
                  </div>
                  <div className="mt-2">

                    <KakaoMap 
                      latitude={currentStatus.location.latitude} 
                      longitude={currentStatus.location.longitude}
                      height="200px"
                    />

                  </div>
                </>
              ) : (
                <p className="text-gray-500">위치 정보 없음</p>
              )}
            </div>
          </div>
        ) : (
          <div className="text-center py-8">
            <div className="text-gray-500 text-lg">🔍 차량 연결 대기 중...</div>
            <p className="text-sm text-gray-400 mt-2">
              Android 앱에서 블루투스 연결을 확인해주세요
            </p>
          </div>
        )}
        
        <button 
          onClick={fetchCurrentStatus}
          className="mt-4 px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
        >
          수동 새로고침
        </button>
      </div>

      <div className="bg-white border border-gray-200 p-4 rounded-lg shadow-sm">
        <h2 className="text-xl font-semibold mb-4">🚗 디바이스별 실시간 추적 현황</h2>
        {deviceTracking.size > 0 ? (
          <div className="space-y-4">
            {Array.from(deviceTracking.entries())
  .filter(([_, info]) => info.deviceName !== 'Unknown Device')
  .map(([deviceKey, info]) => (
              <div key={deviceKey} className="border rounded-lg p-4">
                <div className="flex justify-between items-start mb-3">
                  <div className="flex items-center space-x-3">
                    <div className={`w-3 h-3 rounded-full ${
                      info.isOnline ? 'bg-green-500 animate-pulse' : 'bg-red-500'
                    }`}></div>
                    <div>
                      <h3 className="font-semibold text-lg">{info.deviceName}</h3>
                      <p className="text-sm text-gray-600">기기 ID: {info.deviceId}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className={`px-3 py-1 rounded-full text-white text-sm ${
                      info.isOnline ? 'bg-green-500' : 'bg-red-500'
                    }`}>
                      {info.lastEngineStatus}
                    </span>
                  </div>
                </div>
                
                <div className="grid grid-cols-2 md:grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="text-gray-600">현재 속도</p>
                    <p className="font-mono text-lg">{info.lastSpeed} km/h</p>
                  </div>
                  <div>
                    <p className="text-gray-600">마지막 업데이트</p>
                    <p className="font-mono">{formatTimeOnly(info.lastUpdate)}</p>
                  </div>
                </div>
                
                {info.lastEngineStatus === 'OFF' && info.lastLocation && (
                  <div className="mt-3">
                    <div className="bg-gray-50 p-2 rounded text-xs font-mono">
                      📍 위도: {info.lastLocation.latitude.toFixed(6)}, 
                      경도: {info.lastLocation.longitude.toFixed(6)}
                    </div>
                    <div className="mt-2">
                      <KakaoMap 
                        latitude={info.lastLocation.latitude} 
                        longitude={info.lastLocation.longitude}
                        height="240px"
                        scale={0.5}
                      />
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8">
            <div className="text-gray-500">🔍 연결된 디바이스 없음</div>
            <p className="text-sm text-gray-400 mt-2">
              차량 블루투스 연결 시 실시간으로 표시됩니다
            </p>
          </div>
        )}
        
        <div className="mt-4 flex justify-between items-center">
          <button 
            onClick={fetchStatusHistory}
            className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          >
            데이터 새로고침
          </button>
          <div className="text-sm text-gray-500">
            디바이스별 실시간 통합 표시
          </div>
        </div>
      </div>

      <div className="bg-blue-50 border border-blue-200 p-4 rounded-lg">
        <h3 className="text-lg font-medium text-blue-800 mb-2">💡 사용 방법</h3>
        <ul className="text-sm text-blue-700 space-y-1">
          <li>• 빅스비 루틴을 통해 블루투스 연결 시 자동으로 데이터 수집이 시작됩니다</li>
          <li>• 차량 이동 중에는 1초마다 GPS 위치가 업데이트됩니다</li>
          <li>• 차량 정지 시 자동으로 데이터 전송이 중단됩니다</li>
          <li>• 실시간 데이터는 3초마다 자동으로 새로고침됩니다</li>
          <li>• 디바이스별로 실시간 통합 표시되어 중복 레이블이 제거됩니다</li>
        </ul>
      </div>

    </main>
  );
}