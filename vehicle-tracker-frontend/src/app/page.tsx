'use client';

import { useEffect, useState } from 'react';
import { vehicleApi, VehicleStatus } from './lib/api';

export default function Home() {
  const [currentStatus, setCurrentStatus] = useState<VehicleStatus | null>(null);
  const [statusHistory, setStatusHistory] = useState<VehicleStatus[]>([]);
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);

  // 백엔드 연결 상태 확인
  const checkConnection = async () => {
    const connected = await vehicleApi.testConnection();
    setIsConnected(connected);
    return connected;
  };

  // 현재 상태 조회
  const fetchCurrentStatus = async () => {
    const status = await vehicleApi.getCurrentStatus();
    setCurrentStatus(status);
  };

  // 상태 이력 조회
  const fetchStatusHistory = async () => {
    const history = await vehicleApi.getStatusHistory();
    setStatusHistory(history);
  };

  // 자동 새로고침 (5초마다)
  useEffect(() => {
    const interval = setInterval(() => {
      if (isConnected) {
        fetchCurrentStatus();
        fetchStatusHistory();
      }
    }, 5000);

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
      
      {/* 연결 상태 */}
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
            자동 새로고침: 5초마다
          </div>
        </div>
      </div>

      {/* 현재 차량 상태 */}
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
              <p><strong>블루투스 기기:</strong> {currentStatus.bluetoothDevice}</p>
              <p><strong>시동 상태:</strong> 
                <span className={`ml-2 px-2 py-1 rounded text-white text-sm ${
                  currentStatus.engineStatus === 'ON' ? 'bg-green-500' : 'bg-red-500'
                }`}>
                  {currentStatus.engineStatus}
                </span>
              </p>
              <p><strong>속도:</strong> 
                <span className="ml-2 text-lg font-mono">
                  {Math.round(currentStatus.speed)} km/h
                </span>
              </p>
              <p><strong>마지막 업데이트:</strong> 
                <span className="ml-2 text-sm">
                  {new Date(currentStatus.timestamp.replace(' ', 'T')).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' })}
                </span>
              </p>
            </div>
            
            {/* 위치 정보 */}
            <div className="space-y-2">
              {currentStatus.location ? (
                <>
                  <p><strong>📍 현재 위치:</strong></p>
                  <div className="bg-gray-50 p-3 rounded text-sm font-mono">
                    <p>위도: {currentStatus.location.latitude.toFixed(6)}</p>
                    <p>경도: {currentStatus.location.longitude.toFixed(6)}</p>
                  </div>
                  <div className="mt-2">
                    {/* 카카오맵 표시 영역 (추후 구현) */}
                    <div className="bg-gray-200 h-32 rounded flex items-center justify-center text-gray-500">
                      카카오지도 (준비 중)
                    </div>
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

      {/* 실시간 경로 이력 */}
      <div className="bg-white border border-gray-200 p-4 rounded-lg shadow-sm">
        <h2 className="text-xl font-semibold mb-4">📊 실시간 이동 경로</h2>
        {statusHistory.length > 0 ? (
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {statusHistory.slice().reverse().slice(0, 10).map((status, index) => (
              <div key={index} className="border-b pb-2">
                <div className="flex justify-between items-center">
                  <div className="flex items-center space-x-2">
                    <span className="text-sm font-medium">{status.bluetoothDevice}</span>
                    <span className={`px-2 py-1 rounded text-white text-xs ${
                      status.engineStatus === 'ON' ? 'bg-green-500' : 'bg-red-500'
                    }`}>
                      {status.engineStatus}
                    </span>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-mono">{Math.round(status.speed)} km/h</div>
                    <div className="text-xs text-gray-500">
                      {new Date(status.timestamp.replace(' ', 'T')).toLocaleTimeString('ko-KR', { timeZone: 'Asia/Seoul' })}
                    </div>
                  </div>
                </div>
                {status.location && (
                  <div className="text-xs text-gray-600 mt-1 font-mono">
                    📍 {status.location.latitude.toFixed(4)}, {status.location.longitude.toFixed(4)}
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8">
            <div className="text-gray-500">📋 이동 기록 없음</div>
            <p className="text-sm text-gray-400 mt-2">
              차량 이동 시 실시간으로 경로가 표시됩니다
            </p>
          </div>
        )}
        
        <div className="mt-4 flex justify-between items-center">
          <button 
            onClick={fetchStatusHistory}
            className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          >
            이력 새로고침
          </button>
          <div className="text-sm text-gray-500">
            최근 10개 기록 표시
          </div>
        </div>
      </div>

      {/* 시스템 정보 */}
      <div className="bg-blue-50 border border-blue-200 p-4 rounded-lg">
        <h3 className="text-lg font-medium text-blue-800 mb-2">💡 사용 방법</h3>
        <ul className="text-sm text-blue-700 space-y-1">
          <li>• 빅스비 루틴을 통해 블루투스 연결 시 자동으로 데이터 수집이 시작됩니다</li>
          <li>• 차량 이동 중에는 1초마다 GPS 위치가 업데이트됩니다</li>
          <li>• 차량 정지 시 자동으로 데이터 전송이 중단됩니다</li>
          <li>• 실시간 데이터는 5초마다 자동으로 새로고침됩니다</li>
        </ul>
      </div>
    </main>
  );
}