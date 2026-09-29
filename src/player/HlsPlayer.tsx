import {useEffect,useRef,useState} from 'react';
import Hls from 'hls.js';
export function HlsPlayer({sources,onClose}:{sources:string[];onClose:()=>void}){
 const video=useRef<HTMLVideoElement>(null); const [index,setIndex]=useState(0); const [status,setStatus]=useState('Conectando...');
 useEffect(()=>{const el=video.current;if(!el)return;let hls:Hls|undefined;const url=sources[index];setStatus('Conectando...');
 const fallback=()=>{if(index<sources.length-1){setIndex(v=>v+1)}else setStatus('Fonte indisponível')};
 if(el.canPlayType('application/vnd.apple.mpegurl')){el.src=url;el.play().then(()=>setStatus('')).catch(fallback)}
 else if(Hls.isSupported()){hls=new Hls({enableWorker:true,lowLatencyMode:true,maxBufferLength:20,maxMaxBufferLength:45});hls.loadSource(url);hls.attachMedia(el);hls.on(Hls.Events.MANIFEST_PARSED,()=>el.play().then(()=>setStatus('')).catch(fallback));hls.on(Hls.Events.ERROR,(_,d)=>{if(d.fatal)fallback()})}
 else {el.src=url;el.play().then(()=>setStatus('')).catch(fallback)}
 return()=>hls?.destroy();},[index,sources]);
 return <div className="playerOverlay"><button className="close" onClick={onClose}>✕</button><video ref={video} controls autoPlay playsInline/><div className="playerStatus">{status}</div></div>
}