const GetAPIBaseURL = () => {
    console.log('=== API URL 결정 과정 시작 ===');
    console.log('현재 환경 - typeof window:', typeof window);
    console.log('환경변수 NEXT_PUBLIC_API_URL:', process.env.NEXT_PUBLIC_API_URL);
    
    if (process.env.NEXT_PUBLIC_API_URL) {
        console.log('✅ 환경변수 사용:', process.env.NEXT_PUBLIC_API_URL);
        return process.env.NEXT_PUBLIC_API_URL;
    }

    if(typeof window !== 'undefined') {
        const userAgent = window.navigator.userAgent;
        
        console.log('🔍 전체 User Agent:', userAgent);
        console.log('🔍 Android 포함 여부:', userAgent.includes('Android'));
        console.log('🔍 WebView 포함 여부:', userAgent.includes('WebView'));
        console.log('🔍 wv 포함 여부:', userAgent.includes('wv'));
        
        // 안드로이드 WebView 감지 (더 정확한 조건)
        const isAndroidWebView = userAgent.includes('Android') && 
                                (userAgent.includes('WebView') || userAgent.includes('wv'));
        
        // 또는 단순히 Android만 체크
        const isAndroid = userAgent.includes('Android');
        
        console.log('🔍 Android WebView 감지:', isAndroidWebView);
        console.log('🔍 Android 감지:', isAndroid);
        
        if (isAndroid) {
            const url = 'http://192.168.1.219:8080/api/vehicle';
            console.log('✅ Android 환경 감지! 사용할 URL:', url);
            return url;
        } else {
            console.log('❌ Android 감지 실패, localhost 사용');
        }
    } else {
        console.log('❌ window 객체 없음 (서버사이드 렌더링)');
    }
    
    const defaultUrl = 'http://192.168.1.219:8080/api/vehicle';
    console.log('🔧 기본값 사용 (로컬 환경):', defaultUrl);
    return defaultUrl;
}

const API_BASE_URL = GetAPIBaseURL();
console.log('🎯 최종 선택된 API_BASE_URL:', API_BASE_URL);

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