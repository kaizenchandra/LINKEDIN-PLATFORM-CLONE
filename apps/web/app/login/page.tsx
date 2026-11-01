import {redirect} from 'next/navigation';
export default function Login(){redirect('http://localhost:8081/oauth2/authorization/keycloak');}
