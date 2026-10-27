// Original Telugu-to-Latin rules. No external model or service is used.
(function(root){
'use strict';
const consonants = Object.fromEntries(Object.entries({
'క':'k','ఖ':'kh','గ':'g','ఘ':'gh','ఙ':'ng','చ':'ch','ఛ':'chh','జ':'j','ఝ':'jh','ఞ':'ny','ట':'t','ఠ':'th','డ':'d','ఢ':'dh','ణ':'n','త':'t','థ':'th','ద':'d','ధ':'dh','న':'n','ప':'p','ఫ':'ph','బ':'b','భ':'bh','మ':'m','య':'y','ర':'r','ఱ':'r','ల':'l','ళ':'l','వ':'v','శ':'sh','ష':'sh','స':'s','హ':'h','ఴ':'zh','ౘ':'ts','ౙ':'dz','ౚ':'rr'}));
const vowels={'అ':'a','ఆ':'aa','ఇ':'i','ఈ':'ee','ఉ':'u','ఊ':'oo','ఋ':'ru','ౠ':'roo','ఌ':'lu','ౡ':'loo','ఎ':'e','ఏ':'e','ఐ':'ai','ఒ':'o','ఓ':'o','ఔ':'au'};
const signs={'ా':'aa','ి':'i','ీ':'ee','ు':'u','ూ':'oo','ృ':'ru','ౄ':'roo','ె':'e','ే':'e','ై':'ai','ొ':'o','ో':'o','ౌ':'au','ౢ':'lu','ౣ':'loo'};
const words={
'ఏమండి':'emandi','ఏమండీ':'emandi','ఎలా':'ela','ఉన్నారు':'unnaru','ఉన్నారా':'unnara','ఉన్నాను':'unnanu','ఉన్నావు':'unnavu','ఉన్నావా':'unnava','ఉన్నాం':'unnam','ఉంటారు':'untaru','ఉంటుంది':'untundi','ఉంది':'undi','ఉందా':'unda','ఉన్నది':'unnadi','అండి':'andi','అండీ':'andi','బాగున్నాను':'bagunnanu','బాగున్నారు':'bagunnaru','బాగున్నారా':'bagunnara','బాగుంది':'bagundi','బాగా':'baga','బాగానే':'bagane',
'నేను':'nenu','నాకు':'naaku','నా':'naa','నువ్వు':'nuvvu','నీవు':'neevu','నీకు':'neeku','మీరు':'meeru','మీకు':'meeku','మేము':'memu','మనం':'manam','మన':'mana','మా':'maa','మీ':'mee','వాళ్ళు':'vallu','వాళ్లు':'vallu','వారు':'vaaru','అతను':'atanu','ఆమె':'aame','ఇది':'idi','అది':'adi','ఇవి':'ivi','అవి':'avi',
'ఏం':'em','ఏమి':'emi','ఏమిటి':'emiti','ఏంటి':'enti','ఎందుకు':'enduku','ఎక్కడ':'ekkada','ఎప్పుడు':'eppudu','ఎవరు':'evaru','ఏది':'edi','ఏటిది':'etidi','ఏటి':'eti','ఎరా':'era','ఏరా':'era','ఎంత':'enta','ఎన్ని':'enni','ఏమీ':'emi',
'చేస్తున్నారు':'chestunnaru','చేస్తున్నాను':'chestunnanu','చేస్తున్నావు':'chestunnavu','చేస్తున్నావ్':'chestunnav','చేస్తారు':'chestaru','చేయాలి':'cheyali','చేయాలో':'cheyalo','చెయ్యాలి':'cheyyali','చేయండి':'cheyandi','చెప్పండి':'cheppandi','చెప్పు':'cheppu','చెప్పాలి':'cheppali','చెప్పాను':'cheppanu','చూడు':'choodu','చూడండి':'choodandi','చూశాను':'choosanu','చూసాను':'choosanu','చూద్దాం':'chooddam','వెళ్దాం':'veldam','వెళ్ళాలి':'vellali','వెళ్లాలి':'vellali','వెళ్ళాను':'vellanu','వెళ్లాను':'vellanu','వచ్చాను':'vachanu','వచ్చారు':'vacharu','వస్తాను':'vastanu','వస్తారా':'vastara','రండి':'randi','రావాలి':'raavali','రాయాలి':'raayali','రాయండి':'raayandi',
'తెలుగు':'telugu','తెలుగులో':'telugulo','తెలుగుని':'teluguni','ఇంగ్లీషు':'english','ఇంగ్లీషులో':'englishlo','ఇంగ్లీష్':'english','అక్షరాలు':'aksharaalu','మాటలు':'maatalu','మాట్లాడండి':'maatlaadandi','మాట్లాడుతున్నాను':'maatlaadutunnanu','మాట్లాడాలి':'maatlaadaali','నచ్చింది':'nachindi','నచ్చలేదు':'nachaledu','కావాలి':'kaavali','వద్దు':'vaddu','సరే':'sare','కాదు':'kaadu','అవును':'avunu','లేదు':'ledu','కూడా':'kooda','మళ్ళీ':'malli','మళ్లీ':'malli','చాలా':'chaala','కొంచెం':'konchem','మంచి':'manchi','మంచిది':'manchidi','ఇప్పుడు':'ippudu','అప్పుడు':'appudu','ఇంకా':'inka','ఇక్కడ':'ikkada','అక్కడ':'akkada',
'ఈరోజు':'eeroju','ఇవాళ':'ivala','రేపు':'repu','నిన్న':'ninna','ఉదయం':'udayam','సాయంత్రం':'saayantram','రాత్రి':'raatri','అమ్మ':'amma','నాన్న':'nanna','అన్న':'anna','అక్క':'akka','తమ్ముడు':'tammudu','చెల్లి':'chelli','ప్రేమ':'prema','స్నేహం':'sneham','స్నేహితులు':'snehitulu','నమస్కారం':'namaskaram','ధన్యవాదాలు':'dhanyavaadaalu','శుభాకాంక్షలు':'shubhaakaankshalu','పుట్టినరోజు':'puttinaroju','వినుకో':'vinuko','భోజనం':'bhojanam','తిన్నారా':'tinnara','తిన్నాను':'tinnanu','తిన్నావా':'tinnava','పని':'pani','కోసం':'kosam','అందరూ':'andaru','అందరికీ':'andariki','నిజంగా':'nijanga','నాతో':'naatho','మీతో':'meetho','ఒక':'oka','రెండు':'rendu','మూడు':'moodu','నాలుగు':'naalugu','ఐదు':'aidu',
'యాప్':'app','యాప్స్':'apps','యాప్కు':'appku','మొబైల్':'mobile','ఫోన్':'phone','టూల్':'tool','టూల్స్':'tools','కోడ్':'code','క్రియేట్':'create','న్యాచురల్':'natural','వాట్సాప్':'WhatsApp','వాట్సప్':'WhatsApp','యూట్యూబ్':'YouTube','గూగుల్':'Google','హలో':'hello','థాంక్స్':'thanks','ప్లీజ్':'please','సారీ':'sorry','ఓకే':'okay','టెస్ట్':'test','మెసేజ్':'message','వీడియో':'video','డౌన్లోడ్':'download','డౌన్‌లోడ్':'download'};
function nasal(next){
 if('కఖగఘఙచఛజఝఞటఠడఢణతథదధన'.includes(next)&&next) return 'n';
 return 'm';
}
function raw(word){
 const chars=Array.from(word); let out='';
 for(let i=0;i<chars.length;i++){
  const c=chars[i];
  if(consonants[c]){
   out+=consonants[c];
   let j=i+1; while(chars[j]==='\u200d'||chars[j]==='\u200c')j++;
   if(chars[j]==='్'){i=j;}
   else if(Object.hasOwn(signs,chars[j])){out+=signs[chars[j]];i=j;}
   else out+='a';
  }else if(vowels[c])out+=vowels[c];
  else if(c==='ం'||c==='ఁ'||c==='ఀ'||c==='ఄ')out+=nasal(chars[i+1]);
  else if(c==='ః')out+='h';
  else if(c>='౦'&&c<='౯')out+=String(c.charCodeAt(0)-0x0c66);
  else if(c==='్'||c==='\u200c'||c==='\u200d'||c==='఼'){}
  else if(signs[c])out+=signs[c];
  else out+=c;
 }return out;
}
function convert(text,options={}){
 const custom=options.custom||{};
 let value=text.normalize('NFC').replace(/[\u0c00-\u0c7f][\u0c00-\u0c7f\u200c\u200d]*/gu,w=>Object.hasOwn(custom,w)?custom[w]:(options.style!=='sound'&&Object.hasOwn(words,w)?words[w]:raw(w)));
 if(options.capital!==false)value=value.replace(/(^|[.!?\n]\s*)([a-z])/g,(_,p,c)=>p+c.toUpperCase());
 return value;
}
const api={convert,raw,words};root.Tenglish=api;
if(typeof module!=='undefined')module.exports=api;
})(typeof globalThis!=='undefined'?globalThis:this);
