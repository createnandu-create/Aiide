/* Original alignment helpers: speech always uses the source Telugu. */
(function(root){
'use strict';
function plan(input,options={}){
 const text=input.normalize('NFC');
 const converted=root.Tenglish.convert(text,options);
 let offset=0,index=0;
 const parts=Array.from(text.matchAll(/\s+|\S+/gu),m=>{
  const length=root.Tenglish.convert(m[0],{...options,capital:false}).length;
  const item={source:m[0],text:converted.slice(offset,offset+length),start:m.index,end:m.index+m[0].length,index:/\S/u.test(m[0])?index++:-1};offset+=length;return item;
 });
 const words=parts.filter(p=>p.index>=0),chunks=[];
 let first=0;
 for(let i=0;i<words.length;i++){
  const current=words[i],next=words[i+1];
  if(!next||/[.!?।॥]$/u.test(current.source)||text.slice(current.end,next.start).includes('\n')||next.end-words[first].start>180){
   chunks.push({start:words[first].start,end:current.end,text:text.slice(words[first].start,current.end),first:words[first].index,last:current.index});first=i+1;
  }
 }
 return {text,converted,parts,words,chunks};
}
root.TeluguReading={plan};
if(typeof module!=='undefined')module.exports={plan};
})(typeof globalThis!=='undefined'?globalThis:this);
