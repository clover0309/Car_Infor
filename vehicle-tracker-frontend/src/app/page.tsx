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

  // 테스트 데이터 전송
  const sendTestData = async () => {
    const testStatus = {
      deviceId: 'test-device-001',
      bluetoothDevice: 'Car Audio XYZ',
      engineStatus: 'ON',
      speed: 45.5,
      location: {
        latitude: 37.5665,
        longitude: 126.9780
      }
    };

    const success = await vehicleApi.updateStatus(testStatus);
    if (success) {
      await fetchCurrentStatus();
      await fetchStatusHistory();
      alert('테스트 데이터 전송 완료!');
    } else {
      alert('데이터 전송 실패');
    }
  };

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
      <h1 className="text-3xl font-bold text-center">차량 상태 모니터링 시스템</h1>
      
      {/* 연결 상태 */}
      <div className="bg-gray-100 p-4 rounded-lg">
        <h2 className="text-xl font-semibold mb-2">연결 상태</h2>
        <div className={`inline-block px-3 py-1 rounded-full text-white ${
          isConnected ? 'bg-green-500' : 'bg-red-500'
        }`}>
          {isConnected ? '백엔드 연결됨' : '백엔드 연결 실패'}
        </div>
        <button 
          onClick={checkConnection}
          className="ml-4 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
        >
          연결 확인
        </button>
      </div>

      {/* 현재 차량 상태 */}
      <div className="bg-white border border-gray-200 p-4 rounded-lg">
        <h2 className="text-xl font-semibold mb-4">현재 차량 상태</h2>
        {currentStatus ? (
          <div className="space-y-2">
            <p><strong>기기 ID:</strong> {currentStatus.deviceId}</p>
            <p><strong>블루투스 기기:</strong> {currentStatus.bluetoothDevice}</p>
            <p><strong>시동 상태:</strong> 
              <span className={`ml-2 px-2 py-1 rounded text-white ${
                currentStatus.engineStatus === 'ON' ? 'bg-green-500' : 'bg-red-500'
              }`}>
                {currentStatus.engineStatus}
              </span>
            </p>
            <p><strong>속도:</strong> {currentStatus.speed} km/h</p>
            <p><strong>마지막 업데이트:</strong> {new Date(currentStatus.timestamp).toLocaleString()}</p>
            {currentStatus.location && (
              <p><strong>위치:</strong> {currentStatus.location.latitude}, {currentStatus.location.longitude}</p>
            )}
          </div>
        ) : (
          <p className="text-gray-500">현재 차량 상태 정보가 없습니다.</p>
        )}
        <button 
          onClick={fetchCurrentStatus}
          className="mt-4 px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
        >
          상태 새로고침
        </button>
      </div>

      {/* 테스트 버튼 */}
      <div className="bg-yellow-50 border border-yellow-200 p-4 rounded-lg">
        <h2 className="text-xl font-semibold mb-2">테스트</h2>
        <button 
          onClick={sendTestData}
          className="px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600"
        >
          테스트 데이터 전송
        </button>
      </div>

      {/* 상태 이력 */}
      <div className="bg-white border border-gray-200 p-4 rounded-lg">
        <h2 className="text-xl font-semibold mb-4">상태 이력</h2>
        {statusHistory.length > 0 ? (
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {statusHistory.slice().reverse().map((status, index) => (
              <div key={index} className="border-b pb-2">
                <div className="flex justify-between items-center">
                  <span>{status.deviceId} - {status.bluetoothDevice}</span>
                  <span className={`px-2 py-1 rounded text-white text-sm ${
                    status.engineStatus === 'ON' ? 'bg-green-500' : 'bg-red-500'
                  }`}>
                    {status.engineStatus}
                  </span>
                </div>
                <div className="text-sm text-gray-600">
                  속도: {status.speed} km/h | {new Date(status.timestamp).toLocaleString()}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-gray-500">상태 이력이 없습니다.</p>
        )}
        <button 
          onClick={fetchStatusHistory}
          className="mt-4 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
        >
          이력 새로고침
        </button>
      </div>
    </main>
  );
}