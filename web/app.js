'use strict';
const $=s=>document.querySelector(s),source=$('#source'),result=$('#result');
const STORAGE='telugu-preferences-v2',DICT='telugu-spellings-v1';
let settings={theme:'system',soundEffects:false,haptics:true,motion:true,capital:true,rate:.95,voice:''};
let custom=Object.create(null),style='natural',edited=false,previous='',toastTimer,conversionTimer,composing=false,activeView='convert';
try{const s=JSON.parse(localStorage.getItem(STORAGE)||'{}');if(s&&typeof s==='object')for(const k of Object.keys(settings))if(typeof s[k]===typeof settings[k])settings[k]=s[k];}catch{}
if(!['system','dark','light'].includes(settings.theme))settings.theme='system';settings.rate=Math.min(1.25,Math.max(.65,Number(settings.rate)||.95));
try{const saved=JSON.parse(localStorage.getItem(DICT)||'{}');if(saved&&typeof saved==='object'&&!Array.isArray(saved))for(const [k,v]of Object.entries(saved))if(/^[\u0c00-\u0c7f\u200c\u200d]+$/u.test(k)&&typeof v==='string'&&/^[A-Za-z][A-Za-z' -]{0,79}$/.test(v))custom[k]=v;}catch{}
function persist(){try{localStorage.setItem(STORAGE,JSON.stringify(settings));}catch{toast('Settings ఈ session లో మాత్రమే ఉంటాయి');}}
function toast(text){$('#toastText').textContent=text;$('#toast').classList.add('visible');clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('#toast').classList.remove('visible'),3600);}
let audioContext;
function feedback(success=false){
 if(settings.haptics&&navigator.vibrate)navigator.vibrate(success?[9,30,9]:7);
 if(!settings.soundEffects)return;
 try{const AC=window.AudioContext||window.webkitAudioContext;if(!AC)return;audioContext??=new AC();if(audioContext.state==='suspended')audioContext.resume().catch(()=>{});const t=audioContext.currentTime;
 const notes=success?[650,860]:[620];notes.forEach((f,i)=>{const o=audioContext.createOscillator(),g=audioContext.createGain();o.type='sine';o.frequency.setValueAtTime(f,t+i*.06);g.gain.setValueAtTime(0,t+i*.06);g.gain.linearRampToValueAtTime(.035,t+i*.06+.006);g.gain.exponentialRampToValueAtTime(.0001,t+i*.06+.07);o.connect(g);g.connect(audioContext.destination);o.start(t+i*.06);o.stop(t+i*.06+.08);});}catch{}
}
const darkQuery=window.matchMedia('(prefers-color-scheme: dark)');
function theme(){document.documentElement.dataset.theme=settings.theme==='system'?(darkQuery.matches?'dark':'light'):settings.theme;document.body.classList.toggle('no-motion',!settings.motion);$('meta[name="theme-color"]').content=document.documentElement.dataset.theme==='dark'?'#151723':'#eef1fa';}
darkQuery.addEventListener?.('change',theme);theme();
for(const k of ['theme','soundEffects','haptics','motion','capital']){const el=$('#'+k);if(el.type==='checkbox')el.checked=settings[k];else el.value=settings[k];el.addEventListener('change',()=>{settings[k]=el.type==='checkbox'?el.checked:el.value;persist();theme();if(k==='capital')update();if(k==='soundEffects'&&settings.soundEffects)feedback(true);});}
if(!navigator.vibrate)$('#hapticHelp').textContent='ఈ browser లో vibration support లేదు';
function options(){return{style,custom,capital:settings.capital};}
function controls(){const empty=!result.value.trim();for(const id of ['copy','share','download'])$('#'+id).disabled=empty;$('#outputCount').textContent=Array.from(result.value).length+' letters';$('#count').textContent=Array.from(source.value).length+' / 20,000';updatePlayControls();}
function update(){clearTimeout(conversionTimer);stopReading();edited=false;result.value=Tenglish.convert(source.value,options());$('#conversionState').innerHTML='<i></i> Live conversion';controls();renderReading();}
function queueUpdate(){if(composing)return;if(source.value.length>3000){clearTimeout(conversionTimer);stopReading();$('#conversionState').textContent='Converting…';conversionTimer=setTimeout(update,90);}else update();}
source.addEventListener('compositionstart',()=>{composing=true;});source.addEventListener('compositionend',()=>{composing=false;queueUpdate();});source.addEventListener('input',queueUpdate);
result.addEventListener('input',()=>{stopReading();edited=true;controls();renderReading();});
function setStyle(next){style=next;for(const s of ['natural','sound']){$('#'+s).classList.toggle('selected',style===s);$('#'+s).setAttribute('aria-pressed',String(style===s));}update();}
$('#natural').onclick=()=>setStyle('natural');$('#sound').onclick=()=>setStyle('sound');
function show(view){if(!['convert','listen','words','settings'].includes(view))return;activeView=view;document.querySelectorAll('.view').forEach(v=>{const on=v.id==='view-'+view;v.hidden=!on;v.classList.toggle('active',on);});document.querySelectorAll('.dock [data-view]').forEach(b=>{const on=b.dataset.view===view;b.classList.toggle('active',on);if(on)b.setAttribute('aria-current','page');else b.removeAttribute('aria-current');});if(view!=='convert'&&recognition)recognition.stop();if(view==='listen')renderReading(false);window.scrollTo({top:0,behavior:'instant'});if(location.hash!=='#'+view)history.replaceState(null,'','#'+view);}
document.querySelectorAll('[data-view]').forEach(b=>b.onclick=()=>show(b.dataset.view));window.addEventListener('hashchange',()=>show(location.hash.slice(1)));
$('#clear').onclick=()=>{if(recognition)recognition.stop();previous=source.value;source.value='';$('#undo').disabled=!previous;update();source.focus();};
$('#undo').onclick=()=>{source.value=previous;previous='';$('#undo').disabled=true;update();toast('Text తిరిగి వచ్చింది');};
$('#paste').onclick=async()=>{try{const text=await navigator.clipboard.readText();source.value=text.slice(0,20000);update();toast(text.length>20000?'మొదటి 20,000 characters paste అయ్యాయి':'Text paste అయింది');}catch{source.focus();toast('Text box లో long press → Paste ఎంచుకోండి');}};
async function copy(){try{await navigator.clipboard.writeText(result.value);}catch{result.focus();result.select();result.setSelectionRange(0,result.value.length);let okay=false;try{okay=document.execCommand('copy');}catch{}if(!okay){toast('Long press → Copy ఎంచుకోండి');return;}}feedback(true);toast('Copy అయింది. మీ మాట సిద్ధం!');const span=$('#copy span');span.textContent='Copied!';setTimeout(()=>{span.textContent='Copy text';},1500);}
$('#copy').onclick=copy;
$('#share').onclick=async()=>{if(navigator.share){try{await navigator.share({text:result.value});}catch(e){if(e.name!=='AbortError')toast('Share కాలేదు. Copy text ఉపయోగించండి');}}else{await copy();}};
$('#download').onclick=()=>{const url=URL.createObjectURL(new Blob([result.value],{type:'text/plain;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download='telugu-english-letters.txt';document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),2000);toast('Text download సిద్ధమైంది');};
document.querySelectorAll('[data-example]').forEach(b=>b.onclick=()=>{if(recognition)recognition.stop();source.value=b.dataset.example;update();source.focus({preventScroll:true});});
function saveDictionary(){try{localStorage.setItem(DICT,JSON.stringify(custom));return true;}catch{toast('Save కాలేదు. ఈ session లో spelling పనిచేస్తుంది');return false;}}
function renderWords(){const parent=$('#savedWords');parent.replaceChildren();const entries=Object.entries(custom);$('#wordTotal').textContent=entries.length+' saved';if(!entries.length){const box=document.createElement('div');box.className='empty-state';const icon=document.createElementNS('http://www.w3.org/2000/svg','svg');const use=document.createElementNS('http://www.w3.org/2000/svg','use');use.setAttribute('href','#i-book');icon.append(use);const t=document.createElement('span');t.textContent='ఇక్కడ మీ spellings కనిపిస్తాయి';const s=document.createElement('small');s.textContent='ఒక తెలుగు పదం, మీకు నచ్చిన spelling ఇచ్చి save చేయండి.';box.append(icon,t,s);parent.append(box);return;}
 for(const [k,v]of entries){const row=document.createElement('div');row.className='saved-row';const text=document.createElement('div'),title=document.createElement('strong'),sub=document.createElement('small'),remove=document.createElement('button');title.textContent=k;sub.textContent=v;text.append(title,sub);remove.textContent='×';remove.className='delete-word';remove.type='button';remove.setAttribute('aria-label',k+' spelling తొలగించండి');remove.onclick=()=>{delete custom[k];saveDictionary();renderWords();update();toast('Spelling తొలగించాం');};row.append(text,remove);parent.append(row);}}
$('#wordForm').onsubmit=e=>{e.preventDefault();const w=$('#word').value.trim().normalize('NFC'),s=$('#spelling').value.trim();if(!/^[\u0c00-\u0c7f\u200c\u200d]+$/u.test(w)||!/[\u0c05-\u0c39]/u.test(w)){toast('Spaces లేకుండా ఒక తెలుగు పదం ఇవ్వండి');return;}if(!/^[A-Za-z][A-Za-z' -]{0,79}$/.test(s)){toast('English letters తో spelling ఇవ్వండి');return;}custom[w]=s;const ok=saveDictionary();renderWords();update();$('#wordForm').reset();if(ok){feedback(true);toast('మీ spelling save అయింది');}};
// Recognition uses the browser's speech service, only after an explicit tap.
const Recognition=window.SpeechRecognition||window.webkitSpeechRecognition;let recognition=null,recording=false;
function micState(on){recording=on;$('#mic').classList.toggle('recording',on);$('#mic').setAttribute('aria-pressed',String(on));$('#micLabel').textContent=on?'వింటున్నాం… ఆపడానికి నొక్కండి':'తెలుగులో మాట్లాడండి';}
$('#mic').onclick=()=>{if(recognition){recognition.stop();return;}if(!Recognition){source.focus();toast('కీబోర్డ్‌లో తెలుగు ఎంచుకుని mic నొక్కండి');return;}if(!window.isSecureContext){toast('Voice input కోసం secure HTTPS link తెరవండి');return;}stopReading();const r=new Recognition();recognition=r;r.lang='te-IN';r.continuous=false;r.interimResults=true;r.maxAlternatives=1;const base=source.value.trim(),prefix=base?base+'\n':'';let gotResult=false;
 r.onstart=()=>{micState(true);$('#sourceHelp').textContent='మాట్లాడండి. Audio ని మీ browser speech service process చేయవచ్చు.';};
 r.onresult=e=>{let spoken='';for(let i=0;i<e.results.length;i++)spoken+=e.results[i][0].transcript+' ';gotResult=true;source.value=(prefix+spoken.trim()).slice(0,20000);queueUpdate();};
 r.onerror=e=>{const messages={'not-allowed':'Mic permission రాలేదు. కీబోర్డ్ voice typing వాడండి.','audio-capture':'Microphone అందుబాటులో లేదు.','no-speech':'మాట వినిపించలేదు. మళ్లీ mic నొక్కండి.','network':'Voice service కి connection లేదు. కీబోర్డ్ mic ప్రయత్నించండి.','language-not-supported':'ఈ browser speech service కి తెలుగు support లేదు.'};if(e.error!=='aborted')toast(messages[e.error]||'Voice input ఆగింది. మళ్లీ ప్రయత్నించండి.');};
 r.onend=()=>{if(recognition===r)recognition=null;micState(false);$('#sourceHelp').textContent='Mic పనిచేయకపోతే ఫోన్ కీబోర్డ్‌లో తెలుగు voice typing వాడండి.';if(gotResult)update();};
 try{r.start();}catch{recognition=null;micState(false);toast('Mic ప్రారంభం కాలేదు. కీబోర్డ్ voice typing వాడండి.');}
};
// Sentence-level utterances retain prosody. Boundary events drive word highlights.
const synth='speechSynthesis'in window?window.speechSynthesis:null;const speechSession=synth?TeluguSpeechSession.create({synth}):null;let nextChunkTimer=null,testRunning=false;let voices=[],voiceList=[],readPlan=null,spans=[],state='idle',generation=0,utterance=null,chunkIndex=0,resumeAt=0,activeIndex=-1,watchdog=null,wordBoundaries=false,lastDone=-1,chunkHighlights=[];
function selectedVoice(){const fresh=synth?.getVoices().filter(v=>/^te(?:[-_]|$)/i.test(v.lang))||[];return fresh.find(v=>v.voiceURI===settings.voice)||fresh[0]||null;}
function refreshVoices(){if(!synth){$('#voice').replaceChildren(new Option('ఈ browser లో voice playback లేదు',''));$('#voiceHelp').textContent='మరో browser లో తెరవండి.';$('#quickVoice').textContent='Voice playback not supported';updatePlayControls();return;}
 voices=synth.getVoices();voiceList=voices.filter(v=>/^te(?:[-_]|$)/i.test(v.lang));const old=$('#voice').value;$('#voice').replaceChildren();
 if(!voiceList.length){$('#voice').append(new Option('Telugu voice అందుబాటులో లేదు',''));$('#voiceHelp').textContent='ఫోన్‌లో Telugu text-to-speech voice enable చేసి Refresh voices నొక్కండి.';$('#quickVoice').textContent='Telugu voice setup అవసరం';}
 else{voiceList.forEach(v=>$('#voice').append(new Option(v.name+(v.localService?' · device':''),v.voiceURI)));const wanted=settings.voice||old;if(voiceList.some(v=>v.voiceURI===wanted))$('#voice').value=wanted;else $('#voice').value=voiceList[0].voiceURI;settings.voice=$('#voice').value;$('#voiceHelp').textContent=selectedVoice()?.localService?'Device voice. ఉచ్చారణ quality మీ speech engine మీద ఆధారపడుతుంది.':'Speech provider voice. Internet అవసరం ఉండవచ్చు.';$('#quickVoice').textContent=selectedVoice()?.name||'Telugu voice';}
 updatePlayControls();}
function updatePlayControls(){const can=!!synth&&!!selectedVoice()&&!!source.value.trim();$('#play').disabled=!can;$('#restart').disabled=!can;$('#stop').disabled=state==='idle';$('#listenQuick').disabled=!source.value.trim();$('#testVoice').disabled=!synth||!selectedVoice()||testRunning;}
function renderReading(reset=true){if(!reset&&state!=='idle')return;readPlan=TeluguReading.plan(source.value,options());const parent=$('#karaoke');parent.replaceChildren();spans=[];
 for(const part of readPlan.parts){if(part.index<0)parent.append(document.createTextNode(part.text));else{const span=document.createElement('span');span.textContent=part.text;span.dataset.index=String(part.index);parent.append(span);spans.push(span);}}
 if(!readPlan.words.length)parent.textContent='మీ తెలుగు మాటలు Convert లో రాయండి…';$('#readerCaption').textContent=edited?'Output edit చేశారు. ఇక్కడ మూల తెలుగు conversion చూపించి చదివిస్తాం.':'మూల తెలుగు వాక్యం తెలుగు voice తో వినిపిస్తుంది.';
 if(reset){$('#progress').style.width='0%';activeIndex=-1;lastDone=-1;chunkHighlights=[];}
}
function setPlayer(next){state=next;const playing=next==='playing'||next==='starting';document.body.classList.toggle('speaking',playing);$('#voiceOrb').classList.toggle('playing',playing);$('#play span').textContent=playing?'ఆపి ఉంచండి':next==='paused'?'కొనసాగించండి':'వినండి';$('#play use').setAttribute('href',playing?'#i-pause':'#i-play');$('#playerStatus').textContent=next==='playing'?'చదువుతోంది…':next==='starting'?'Voice starting…':next==='paused'?'ఆపి ఉంచాం':'Ready to listen';updatePlayControls();}
function clearHighlights(){for(const s of spans)s.classList.remove('current','current-chunk','done');activeIndex=-1;lastDone=-1;chunkHighlights=[];}
function stopReading(){if(typeof state==='undefined'||state==='idle')return;generation++;clearTimeout(watchdog);clearTimeout(nextChunkTimer);speechSession?.stop();utterance=null;resumeAt=0;chunkIndex=0;setPlayer('idle');clearHighlights();$('#progress').style.width='0%';}
function wordAt(offset){let low=0,high=readPlan.words.length-1;while(low<high){const mid=(low+high)>>1;if(readPlan.words[mid].end<=offset)low=mid+1;else high=mid;}return readPlan.words[low];}
function highlight(index,chunk){if(activeIndex>=0)spans[activeIndex]?.classList.remove('current');if(index<lastDone)for(let i=index;i<=lastDone;i++)spans[i]?.classList.remove('done');for(let i=lastDone+1;i<index;i++)spans[i]?.classList.add('done');lastDone=index-1;activeIndex=index;
 for(const i of chunkHighlights)spans[i]?.classList.remove('current-chunk');chunkHighlights=[];
 if(wordBoundaries)spans[index]?.classList.add('current');else for(let i=chunk.first;i<=chunk.last;i++){spans[i]?.classList.add('current-chunk');chunkHighlights.push(i);}
 $('#progress').style.width=Math.round((index+1)/Math.max(1,readPlan.words.length)*100)+'%';$('#highlightMode').textContent=wordBoundaries?'Word highlight':'Sentence highlight';const current=spans[index];if(activeView==='listen'&&current){const parent=$('#karaoke');if(current.offsetTop-parent.offsetTop<parent.scrollTop||current.offsetTop-parent.offsetTop>parent.scrollTop+parent.clientHeight-40)parent.scrollTop=Math.max(0,current.offsetTop-parent.offsetTop-35);}}
function readChunk(token){if(token!==generation||!readPlan)return;const chunk=readPlan.chunks[chunkIndex];if(!chunk){clearTimeout(watchdog);setPlayer('idle');utterance=null;resumeAt=0;$('#progress').style.width='100%';spans.forEach(s=>{s.classList.remove('current','current-chunk');s.classList.add('done');});$('#playerStatus').textContent='చదవడం పూర్తయింది';return;}
 const start=Math.max(chunk.start,resumeAt),text=readPlan.text.slice(start,chunk.end);if(!text.trim()){chunkIndex++;resumeAt=0;readChunk(token);return;}
 wordBoundaries=false;
 speechSession.start({text,voice:selectedVoice(),rate:settings.rate,
 onStart:({fallback})=>{if(token!==generation)return;setPlayer('playing');$('#speechError').hidden=true;if(fallback){$('#voiceHelp').textContent='System Telugu voice తో playback కొనసాగుతోంది (1.00×).';}highlight(wordAt(start)?.index??chunk.first,chunk);},
 onBoundary:e=>{if(token!==generation||e.name!=='word')return;wordBoundaries=true;const w=wordAt(start+e.charIndex);if(w){resumeAt=w.start;highlight(w.index,chunk);}},
 onEnd:()=>{if(token!==generation)return;chunkIndex++;resumeAt=0;nextChunkTimer=setTimeout(()=>readChunk(token),90);},
 onRetry:()=>{if(token===generation)$('#playerStatus').textContent='System Telugu voice ప్రయత్నిస్తున్నాం…';},
 onError:e=>{if(token!==generation)return;stopReading();reportSpeechError(e.error);}
 });
}
function toggleReading(restart=false){if(testRunning){speechSession.stop();testRunning=false;$('#testVoiceStatus').textContent='Test ఆపాం';updatePlayControls();}if(!synth||!selectedVoice()){refreshVoices();toast('ఫోన్‌లో Telugu voice enable చేసి Refresh voices నొక్కండి.');return;}if(!source.value.trim())return;if(recognition){recognition.stop();toast('మాట్లాడటం ఆగిన తర్వాత వినండి నొక్కండి.');return;}
 if((state==='playing'||state==='starting')&&!restart){generation++;clearTimeout(watchdog);clearTimeout(nextChunkTimer);speechSession.stop();if(!wordBoundaries)resumeAt=readPlan.chunks[chunkIndex]?.start||0;setPlayer('paused');$('#readerCaption').textContent='కొనసాగిస్తే చివరి highlight చేసిన పదం / వాక్యం నుంచి చదువుతుంది.';return;}
 const isResume=state==='paused'&&!restart;if(!isResume){stopReading();renderReading();chunkIndex=0;resumeAt=0;}generation++;const token=generation;$('#speechError').hidden=true;setPlayer('starting');readChunk(token);
}
function reportSpeechError(code){const messages={
 'not-allowed':['Browser playback అనుమతించలేదు','ఈ లింక్‌ను Chrome లో నేరుగా తెరిచి Test Telugu voice నొక్కండి.'],
 'audio-busy':['Audio service busy గా ఉంది','Calls లేదా మరో audio app ఆపి Test Telugu voice నొక్కండి.'],
 'audio-hardware':['Audio output అందుబాటులో లేదు','Media volume, Bluetooth output చూసి మళ్లీ ప్రయత్నించండి.'],
 'network':['Speech service కి connection లేదు','Internet connection చూసి, Telugu voice data download అయిందో చూడండి.'],
 'voice-unavailable':['ఎంపిక చేసిన voice load కాలేదు','Android Settings → Text-to-speech లో Telugu voice data ఎంచుకుని engine preview వినండి.'],
 'language-unavailable':['తెలుగు voice data సిద్ధంగా లేదు','Android Settings లో Text-to-speech కోసం వెతికి Telugu voice data download చేయండి.'],
 'synthesis-failed':['ఫోన్ speech engine playback విఫలమైంది','Voice పేరు కనిపించినా engine audio ఇవ్వలేదు. Android Settings లో Text-to-speech preview పరీక్షించండి. Speech Services update చేసి, ఈ లింక్‌ను Chrome లో తెరవండి.'],
 'synthesis-unavailable':['Speech engine అందుబాటులో లేదు','Android Settings → Text-to-speech లో speech engine ఎంచుకుని Telugu voice data సిద్ధం చేయండి.'],
 'start-timeout':['Speech engine నుంచి స్పందన రాలేదు','Text-to-speech settings లో Telugu preview పరీక్షించి Chrome లో మళ్లీ తెరవండి.'],
 'playback-timeout':['Voice playback మధ్యలో ఆగిపోయింది','చిన్న తెలుగు వాక్యంతో Test Telugu voice ప్రయత్నించండి.']
 };const [title,advice]=messages[code]||['Voice playback ఆగింది','Test Telugu voice నొక్కండి. సమస్య కొనసాగితే క్రింద error code తో screenshot పంపండి.'];$('#speechErrorTitle').textContent=title;$('#speechErrorAdvice').textContent=advice;$('#speechErrorCode').textContent='Error: '+code+' · '+(selectedVoice()?.name||'System Telugu');$('#speechError').hidden=false;$('#playerStatus').textContent='Playback failed';toast(title);}
$('#testVoice').onclick=()=>{stopReading();speechSession?.stop();clearTimeout(nextChunkTimer);testRunning=true;updatePlayControls();$('#speechError').hidden=true;$('#testVoiceStatus').textContent='“నమస్కారం” voice test ప్రారంభిస్తున్నాం…';speechSession.start({text:'నమస్కారం. మీరు ఎలా ఉన్నారు?',voice:selectedVoice(),rate:1,onStart:()=>{$('#testVoiceStatus').textContent='Test voice చదువుతోంది…';},onRetry:()=>{$('#testVoiceStatus').textContent='System Telugu voice తో మళ్లీ ప్రయత్నిస్తున్నాం…';},onEnd:()=>{testRunning=false;updatePlayControls();$('#testVoiceStatus').textContent='Voice test పూర్తయింది. ఇప్పుడు వినండి నొక్కండి.';},onError:e=>{testRunning=false;updatePlayControls();$('#testVoiceStatus').textContent='Voice test విఫలమైంది.';reportSpeechError(e.error);}});};
$('#play').onclick=()=>toggleReading();$('#restart').onclick=()=>toggleReading(true);$('#stop').onclick=()=>stopReading();$('#listenQuick').onclick=()=>{show('listen');toggleReading();};
$('#voice').onchange=()=>{if(testRunning){speechSession.stop();testRunning=false;$('#testVoiceStatus').textContent='';}stopReading();settings.voice=$('#voice').value;persist();refreshVoices();};
$('#refreshVoices').onclick=()=>{refreshVoices();toast(voiceList.length?'Telugu voices ready':'ఇంకా Telugu voice కనిపించలేదు. కింద సూచనలు చూడండి.');};
$('#rate').value=String(settings.rate);$('#rateValue').value=settings.rate.toFixed(2)+'×';$('#rate').addEventListener('input',()=>{$('#rateValue').value=Number($('#rate').value).toFixed(2)+'×';});$('#rate').addEventListener('change',()=>{stopReading();settings.rate=Number($('#rate').value);persist();});
if(synth)synth.addEventListener('voiceschanged',refreshVoices);refreshVoices();
// Install and update use standard browser facilities, without a forced reload.
let installPrompt=null,workerRegistration=null;
window.addEventListener('beforeinstallprompt',e=>{e.preventDefault();installPrompt=e;$('#installHelp').textContent='ఈ browser లో Install అందుబాటులో ఉంది.';});
$('#install').onclick=async()=>{if(installPrompt){try{await installPrompt.prompt();await installPrompt.userChoice;}catch{}installPrompt=null;return;}const ios=/iPad|iPhone|iPod/.test(navigator.userAgent);const message=ios?'Safari Share menu → Add to Home Screen ఎంచుకోండి.':'Browser menu ⋮ → Add to Home screen / Install app ఎంచుకోండి.';$('#installHelp').textContent=message;toast(message);};
$('#updateApp').onclick=async()=>{try{if(workerRegistration)await workerRegistration.update();toast('తాజా వెర్షన్ కోసం పేజీ reload చేయండి. మీ input ముందుగా copy చేసుకోండి.');}catch{toast('Connection లేదు. Online వచ్చినప్పుడు మళ్లీ ప్రయత్నించండి.');}};
function connection(){$('#connection').innerHTML=navigator.onLine?'<i></i> Ready':'<i></i> Offline';}
window.addEventListener('online',connection);window.addEventListener('offline',connection);connection();
document.addEventListener('click',e=>{const b=e.target.closest('button');if(b&&!b.disabled&&b.id!=='copy')feedback();});
document.addEventListener('visibilitychange',()=>{if(document.hidden){if(testRunning){speechSession?.stop();testRunning=false;$('#testVoiceStatus').textContent='పేజీ background కి వెళ్లడంతో test ఆపాం.';updatePlayControls();}if(recognition)recognition.stop();if(state==='playing'||state==='starting'){generation++;clearTimeout(watchdog);clearTimeout(nextChunkTimer);speechSession?.stop();if(!wordBoundaries)resumeAt=readPlan?.chunks[chunkIndex]?.start||0;setPlayer('paused');}}});window.addEventListener('pagehide',()=>{generation++;clearTimeout(nextChunkTimer);speechSession?.stop();recognition?.abort();});
micState(false);renderWords();update();show(['convert','listen','words','settings'].includes(location.hash.slice(1))?location.hash.slice(1):'convert');
if('serviceWorker'in navigator)navigator.serviceWorker.register('./sw.js').then(r=>{workerRegistration=r;}).catch(()=>{});
