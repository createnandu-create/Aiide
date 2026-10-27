/* Browser speech lifecycle adapter. Injectable timers/engine permit regression tests. */
(function(root){
'use strict';
function create({synth,makeUtterance=text=>new SpeechSynthesisUtterance(text),schedule=setTimeout,unschedule=clearTimeout}){
 let active=null;
 function stop(){const current=active;active=null;if(current)current.cancel();}
 function start(config){stop();let closed=false,started=false,attempt=0,serial=0,timer=null,utterance=null;
  const clear=()=>{if(timer!==null)unschedule(timer);timer=null;};
  const finishError=code=>{if(closed)return;closed=true;clear();if(active===handle)active=null;if(code.endsWith('timeout'))try{synth.cancel();}catch{}config.onError?.({error:code,attempt:attempt+1});};
  const retryable=new Set(['synthesis-failed','synthesis-unavailable','voice-unavailable','language-unavailable','audio-busy','interrupted','canceled','start-timeout']);
  const failed=(code,id)=>{if(closed||id!==serial)return;clear();if(!started&&attempt===0&&retryable.has(code)){attempt=1;serial++;config.onRetry?.(code);try{synth.cancel();}catch{}timer=schedule(run,200);return;}finishError(code);};
  const handle={cancel(){if(closed)return;closed=true;serial++;clear();try{synth.cancel();}catch{}},get utterance(){return utterance;}};
  active=handle;
  function run(){if(closed)return;clear();started=false;const id=++serial;let u;
   try{u=makeUtterance(config.text);utterance=u;const voice=attempt===0?config.voice:null;u.lang=voice?.lang?voice.lang.replace(/_/g,'-'):'te-IN';if(voice)u.voice=voice;u.rate=attempt===0?config.rate||1:1;u.pitch=1;u.volume=1;
    u.onstart=()=>{if(closed||id!==serial)return;started=true;clear();config.onStart?.({fallback:attempt>0});timer=schedule(()=>failed('playback-timeout',id),Math.max(25000,config.text.length*280/Math.max(.65,u.rate)));};
    u.onboundary=e=>{if(!closed&&id===serial)config.onBoundary?.(e);};
    u.onend=()=>{if(closed||id!==serial)return;closed=true;clear();if(active===handle)active=null;config.onEnd?.({fallback:attempt>0});};
    u.onerror=e=>failed(e.error||'synthesis-failed',id);
    timer=schedule(()=>failed('start-timeout',id),12000);
    // No cancel() on an idle engine. Preserve the user's direct play gesture.
    synth.speak(u);
   }catch(e){failed(e.name==='NotAllowedError'?'not-allowed':'synthesis-failed',id);}
  }
  run();return handle;
 }
 return{start,stop};
}
root.TeluguSpeechSession={create};if(typeof module!=='undefined')module.exports={create};
})(typeof globalThis!=='undefined'?globalThis:this);
