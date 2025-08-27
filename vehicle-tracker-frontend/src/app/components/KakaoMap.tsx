'use client';

import { useEffect, useRef } from 'react';

interface KakaoMapProps {
  latitude: number;
  longitude: number;
  width?: string;
  height?: string;
  scale?: number;
}

declare global {
  interface Window {
    kakao: any;
  }
}

export default function KakaoMap({ 
  latitude, 
  longitude, 
  width = '100%', 
  height = '200px',
  scale = 1
}: KakaoMapProps) {
  const mapContainer = useRef<HTMLDivElement>(null);
  const mapRef = useRef<any>(null);
  
  // scale 계산.
  const scaledWidth = scale === 1 ? width : `calc(${width} * ${scale})`;
  const scaledHeight = scale === 1 ? height : `calc(${height} * ${scale})`;

  useEffect(() => {
    // 카카오 지도 API가 로드되었는지 확인
    if (!window.kakao || !window.kakao.maps) {
      console.error('카카오 지도 API가 로드되지 않았습니다.');
      return;
    }

    const initMap = () => {
      // 지도 옵션 설정
      const options = {
        center: new window.kakao.maps.LatLng(latitude, longitude),
        level: 3 
      };

      // 지도 생성
      const map = new window.kakao.maps.Map(mapContainer.current, options);
      mapRef.current = map;

      // 마커 생성
      const markerPosition = new window.kakao.maps.LatLng(latitude, longitude);
      const marker = new window.kakao.maps.Marker({
        position: markerPosition
      });

      // 마커를 지도에 표시
      marker.setMap(map);

      // 인포윈도우 생성
      const infowindow = new window.kakao.maps.InfoWindow({
        content: `
          <div style="padding:5px; font-size:12px; width:200px;">
            <strong>현재 위치</strong><br/>
            위도: ${latitude.toFixed(6)}<br/>
            경도: ${longitude.toFixed(6)}
          </div>
        `
      });

      // 마커 클릭 시 인포윈도우 표시
      window.kakao.maps.event.addListener(marker, 'click', function() {
        infowindow.open(map, marker);
      });

      // 초기 렌더 후 컨테이너 크기 확정에 맞춰 타일 재배치
      setTimeout(() => {
        try {
          map.relayout();
          map.setCenter(new window.kakao.maps.LatLng(latitude, longitude));
        } catch (e) {
          console.warn('map.relayout 중 예외:', e);
        }
      }, 300);
    };

    // Next.js + WebView 환경에서 안전하게 초기화
    if (typeof window.kakao.maps.load === 'function') {
      window.kakao.maps.load(initMap);
    } else {
      initMap();
    }

  }, [latitude, longitude]);

  // 윈도우 리사이즈 시 타일 재배치
  useEffect(() => {
    const handleResize = () => {
      if (window.kakao && mapRef.current) {
        try {
          mapRef.current.relayout();
          mapRef.current.setCenter(new window.kakao.maps.LatLng(latitude, longitude));
        } catch (e) {
          console.warn('resize 처리 중 예외:', e);
        }
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [latitude, longitude]);

  return (
    <div 
      ref={mapContainer} 
      style={{ width: scaledWidth, height: scaledHeight }}
      className="rounded-lg border border-gray-300"
    />
  );
}
