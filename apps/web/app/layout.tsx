import './style.css';
import './platform.css';
export const metadata={title:'Meridian | Professional community',description:'Build your professional profile and connect with your network.'};
const themeBootstrap="try{var t=localStorage.getItem('meridian-theme');if(t==='light'||t==='dark'||t==='system')document.documentElement.dataset.theme=t;}catch(e){}";
export default function Layout({children}:{children:React.ReactNode}){return <html lang="en" suppressHydrationWarning><head><script dangerouslySetInnerHTML={{__html:themeBootstrap}}/></head><body>{children}</body></html>}
