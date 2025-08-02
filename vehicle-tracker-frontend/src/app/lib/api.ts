const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://10.0.2.2:8080/api/vehicle';

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