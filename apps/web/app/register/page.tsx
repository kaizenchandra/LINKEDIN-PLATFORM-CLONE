import {redirect} from 'next/navigation';
export default function Register(){redirect('http://localhost:8081/oauth2/authorization/keycloak?screen=register');}
