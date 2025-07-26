const API_BASE_URL = 'http://localhost:8080/api/vehicle';

export interface VehicleStatus {
    deviceId: string;
    bluetoothDevice: string;
    engineStatus: string;
    speed: number;
    timestamp: string;

location?: {
    latitude: number;
    longitude: number;
    };
}

export const vehicleApi = {
    // 현재 차량 상태 조회
    getCurrentStatus: async (): Promise<VehicleStatus | null> => {
    try {
        const response = await fetch(`${API_BASE_URL}/current`);
    if (!response.ok) {
        throw new Error('Failed to fetch current status');
}

    const data = await response.json();

// 백엔드에서 반환하는 새로운 형식 처리
    if (data.hasData && data.status) {
        return data.status;
}

    return null;
    } catch (error) {
        console.error('Error fetching current status:', error);
        return null;
    }
},

// 차량 상태 이력 조회
    getStatusHistory: async (): Promise<VehicleStatus[]> => {
    try {
        const response = await fetch(`${API_BASE_URL}/history`);
        if (!response.ok) {
            throw new Error('Failed to fetch status history');
}

const data = await response.json();
    return data;
} catch (error) {
    console.error('Error fetching status history:', error);
        return [];
    }
},

  // 차량 상태 업데이트 (테스트용)
  updateStatus: async (status: Omit<VehicleStatus, 'timestamp'>): Promise<boolean> => {
    try {
      console.log('전송할 데이터:', JSON.stringify(status, null, 2)); // 디버깅용 로그
      
      const response = await fetch(`${API_BASE_URL}/status`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(status),
      });
      
      console.log('응답 상태:', response.status); // 디버깅용 로그
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('서버 응답 에러:', errorText);
        return false;
      }
      
      const result = await response.json();
      console.log('서버 응답:', result); // 디버깅용 로그
      
      return true;
    } catch (error) {
      console.error('Error updating status:', error);
      return false;
    }
  },

  // 백엔드 연결 테스트
  testConnection: async (): Promise<boolean> => {
    try {
      const response = await fetch(`${API_BASE_URL}/test`);
      return response.ok;
    } catch (error) {
      console.error('Error testing connection:', error);
      return false;
    }
  }
};