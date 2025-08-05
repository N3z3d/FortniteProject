import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { UserContextService } from '../services/user-context.service';

export const AuthInterceptor: HttpInterceptorFn = (request, next) => {
    const userContextService = inject(UserContextService);
    let currentUser = userContextService.getCurrentUser();
    
    // Si aucun utilisateur connecté, ne pas ajouter de paramètre user
    // Laisser l'API gérer l'authentification et retourner une erreur 401
    if (!currentUser) {
        console.warn('Aucun utilisateur connecté - requête envoyée sans authentification');
        return next(request);
    }
   
    // Vérifier si le paramètre user n'existe pas déjà
    const url = new URL(request.url);
    if (!url.searchParams.has('user')) {
        // Ajouter l'utilisateur connecté comme paramètre de requête
        const modifiedRequest = request.clone({
            setParams: {
                user: currentUser.username
            }
        });
        
        return next(modifiedRequest);
    }
  
    return next(request);
}; 