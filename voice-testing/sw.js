/* Telugu Voice Studio: best-effort offline resource cache.
   Models, WASM and voice files are downloaded only on demand. Browser storage
   may be cleared at any time. This cannot make an online-only Android voice offline. */
const CACHE="telugu-voice-local-v1";
self.addEventListener("install",event=>{
 event.waitUntil((async()=>{
  const cache=await caches.open(CACHE);
  try{await cache.addAll(["./offline.html","./manifest.webmanifest","./sw.js"])}catch(err){console.warn("Offline precache:",err)}
  await self.skipWaiting();
 })());
});
self.addEventListener("activate",event=>{
 event.waitUntil((async()=>{
  const keys=await caches.keys();await Promise.all(keys.filter(k=>k.startsWith("telugu-voice-local-")&&k!==CACHE).map(k=>caches.delete(k)));
  await self.clients.claim();
 })());
});
self.addEventListener("fetch",event=>{
 if(event.request.method!=="GET")return;
 const url=new URL(event.request.url),same=url.origin===self.location.origin;
 const model=url.hostname==="huggingface.co"&&url.pathname.includes("/rhasspy/piper-voices/");
 const libs=url.hostname==="cdn.jsdelivr.net"&&url.pathname.includes("/piper-tts.github.io");
 if(!same&&!model&&!libs)return;
 event.respondWith((async()=>{
  const cache=await caches.open(CACHE);
  const isPage=same&&(event.request.mode==="navigate"||url.pathname.endsWith(".webmanifest"));
  if(isPage){
   try{
    const fresh=await fetch(event.request);
    if(fresh.ok){try{await cache.put(event.request,fresh.clone())}catch(e){}}
    return fresh;
   }catch(e){
    const cached=await cache.match(event.request)||await cache.match("./offline.html");
    if(cached)return cached;
    throw e;
   }
  }
  const found=await cache.match(event.request);if(found)return found;
  const response=await fetch(event.request);
  if(response.ok){try{await cache.put(event.request,response.clone())}catch(e){console.warn("Cache full or unavailable:",e)}}
  return response;
 })());
});