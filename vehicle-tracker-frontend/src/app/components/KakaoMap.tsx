'use client';

import { useEffect, useRef } from 'react';

interface KakaoMapProps {
  latitude: number;
  longitude: number;
  width?: string;
  height?: string;
  scale?: number; // 1이면 원본, 0.5면 절반 크기
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
  
  // scale 값에 따라 실제 렌더링 크기를 계산
  const scaledWidth = scale === 1 ? width : `calc(${width} * ${scale})`;
  const scaledHeight = scale === 1 ? height : `calc(${height} * ${scale})`;

  useEffect(() => {
    // 카카오 지도 API가 로드되었는지 확인
    if (!window.kakao || !window.kakao.maps) {
      console.error('카카오 지도 API가 로드되지 않았습니다.');
      return;
    }

    // 지도 옵션 설정
    const options = {
      center: new window.kakao.maps.LatLng(latitude, longitude),
      level: 3 // 지도 확대 레벨 (1~14)
    };

    // 지도 생성
    const map = new window.kakao.maps.Map(mapContainer.current, options);

    // 마커 생성
    const markerPosition = new window.kakao.maps.LatLng(latitude, longitude);
    const marker = new window.kakao.maps.Marker({
      position: markerPosition
    });

    // 마커를 지도에 표시
    marker.setMap(map);

    // 인포윈도우 생성 (위치 정보 표시)
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

  }, [latitude, longitude]);

  return (
    <div 
      ref={mapContainer} 
      style={{ width: scaledWidth, height: scaledHeight }}
      className="rounded-lg border border-gray-300"
    />
  );
}
