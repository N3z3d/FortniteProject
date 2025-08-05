import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * SPRINT 4 - IMAGE OPTIMIZATION SERVICE
 * 
 * Service pour optimisation des images avec:
 * - Support WebP avec fallbacks automatiques
 * - Lazy loading intelligent avec Intersection Observer
 * - Responsive images avec srcset
 * - Compression dynamique selon la connection
 * - Cache optimisé pour performance globale
 */
@Injectable({
  providedIn: 'root'
})
export class ImageOptimizationService {

  private readonly supportsWebP: boolean;
  private readonly connectionSpeed: string;
  private readonly intersectionObserver: IntersectionObserver | null = null;

  constructor() {
    this.supportsWebP = this.checkWebPSupport();
    this.connectionSpeed = this.detectConnectionSpeed();
    this.initializeIntersectionObserver();
  }

  /**
   * Génère l'URL optimisée pour une image avec tous les paramètres
   */
  getOptimizedImageUrl(
    imagePath: string, 
    options: ImageOptimizationOptions = {}
  ): OptimizedImageUrls {
    const {
      width = 300,
      height,
      quality = this.getOptimalQuality(),
      format = 'auto',
      lazy = true,
      responsive = true
    } = options;

    const baseUrl = environment.production && environment.cdn?.enabled 
      ? environment.cdn.assets.images.baseUrl 
      : '/assets/images';

    // Génération des URLs pour différents formats
    const webpUrl = `${baseUrl}/${imagePath}?format=webp&w=${width}${height ? `&h=${height}` : ''}&q=${quality}`;
    const fallbackUrl = `${baseUrl}/${imagePath}?w=${width}${height ? `&h=${height}` : ''}&q=${quality}`;
    
    // Génération du srcset pour responsive images
    const srcset = responsive ? this.generateSrcSet(imagePath, width, quality) : '';
    
    // Placeholder pour lazy loading
    const placeholder = environment.cdn?.assets?.images?.placeholder || 
      'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMSIgaGVpZ2h0PSIxIiB2aWV3Qm94PSIwIDAgMSAxIiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9IiNGNUY1RjUiLz48L3N2Zz4=';

    return {
      src: this.supportsWebP ? webpUrl : fallbackUrl,
      srcset,
      placeholder,
      webp: webpUrl,
      fallback: fallbackUrl,
      lazy
    };
  }

  /**
   * Génère un srcset responsive pour différentes tailles
   */
  private generateSrcSet(imagePath: string, baseWidth: number, quality: number): string {
    const sizes = environment.cdn?.assets?.images?.sizes || [150, 300, 600, 1200];
    const baseUrl = environment.production && environment.cdn?.enabled 
      ? environment.cdn.assets.images.baseUrl 
      : '/assets/images';

    const format = this.supportsWebP ? 'webp' : 'jpg';
    
    return sizes
      .filter((size: number) => size <= baseWidth * 2) // Évite les images trop grandes
      .map((size: number) => `${baseUrl}/${imagePath}?format=${format}&w=${size}&q=${quality} ${size}w`)
      .join(', ');
  }

  /**
   * Détecte le support WebP du navigateur
   */
  private checkWebPSupport(): boolean {
    try {
      const canvas = document.createElement('canvas');
      canvas.width = 1;
      canvas.height = 1;
      return canvas.toDataURL('image/webp', 0.1).indexOf('data:image/webp') === 0;
    } catch {
      return false;
    }
  }

  /**
   * Détecte la vitesse de connexion pour adapter la qualité
   */
  private detectConnectionSpeed(): string {
    if ('connection' in navigator) {
      const connection = (navigator as any).connection;
      const effectiveType = connection?.effectiveType;
      
      switch (effectiveType) {
        case 'slow-2g':
        case '2g':
          return 'slow';
        case '3g':
          return 'medium';
        case '4g':
          return 'fast';
        default:
          return 'medium';
      }
    }
    return 'medium';
  }

  /**
   * Calcule la qualité optimale selon la connexion
   */
  private getOptimalQuality(): number {
    switch (this.connectionSpeed) {
      case 'slow':
        return 60; // Qualité réduite pour connexions lentes
      case 'medium':
        return 75; // Qualité équilibrée
      case 'fast':
        return 85; // Haute qualité pour connexions rapides
      default:
        return 75;
    }
  }

  /**
   * Initialise l'Intersection Observer pour lazy loading
   */
  private initializeIntersectionObserver(): void {
    if (!('IntersectionObserver' in window)) {
      return; // Fallback: pas de lazy loading sur les vieux navigateurs
    }

    const options = {
      root: null,
      rootMargin: environment.performance?.lazyLoading?.rootMargin || '50px',
      threshold: environment.performance?.lazyLoading?.intersectionThreshold || 0.1
    };

    (this as any).intersectionObserver = new IntersectionObserver((entries: IntersectionObserverEntry[]) => {
      entries.forEach((entry: IntersectionObserverEntry) => {
        if (entry.isIntersecting) {
          const img = entry.target as HTMLImageElement;
          this.loadImage(img);
          this.intersectionObserver?.unobserve(img);
        }
      });
    }, options);
  }

  /**
   * Active le lazy loading sur un élément image
   */
  enableLazyLoading(img: HTMLImageElement, optimizedUrls: OptimizedImageUrls): void {
    if (!this.intersectionObserver || !optimizedUrls.lazy) {
      // Charger immédiatement si pas de support ou lazy loading désactivé
      this.loadImageUrls(img, optimizedUrls);
      return;
    }

    // Définir le placeholder
    img.src = optimizedUrls.placeholder;
    img.setAttribute('data-src', optimizedUrls.src);
    img.setAttribute('data-srcset', optimizedUrls.srcset);
    
    // Observer l'image pour lazy loading
    this.intersectionObserver.observe(img);
  }

  /**
   * Charge l'image réelle quand elle devient visible
   */
  private loadImage(img: HTMLImageElement): void {
    const src = img.getAttribute('data-src');
    const srcset = img.getAttribute('data-srcset');
    
    if (src) {
      // Précharge l'image pour éviter le flash
      const preloader = new Image();
      preloader.onload = () => {
        img.src = src;
        if (srcset) {
          img.srcset = srcset;
        }
        img.classList.add('loaded');
      };
      preloader.src = src;
    }
  }

  /**
   * Charge les URLs d'image optimisées directement
   */
  private loadImageUrls(img: HTMLImageElement, optimizedUrls: OptimizedImageUrls): void {
    img.src = optimizedUrls.src;
    if (optimizedUrls.srcset) {
      img.srcset = optimizedUrls.srcset;
    }
    img.classList.add('loaded');
  }

  /**
   * Précharge des images critiques
   */
  preloadCriticalImages(imagePaths: string[]): void {
    imagePaths.forEach((path: string) => {
      const optimizedUrls = this.getOptimizedImageUrl(path, { 
        lazy: false, 
        quality: 85 
      });
      
      const link = document.createElement('link');
      link.rel = 'preload';
      link.as = 'image';
      link.href = optimizedUrls.src;
      
      // Ajouter support WebP si disponible
      if (this.supportsWebP) {
        link.setAttribute('imagesrcset', optimizedUrls.srcset);
        link.setAttribute('imagesizes', '(max-width: 600px) 300px, (max-width: 1200px) 600px, 1200px');
      }
      
      document.head.appendChild(link);
    });
  }
}

// Interfaces pour la configuration
export interface ImageOptimizationOptions {
  width?: number;
  height?: number;
  quality?: number;
  format?: 'auto' | 'webp' | 'jpg' | 'png';
  lazy?: boolean;
  responsive?: boolean;
}

export interface OptimizedImageUrls {
  src: string;
  srcset: string;
  placeholder: string;
  webp: string;
  fallback: string;
  lazy: boolean;
}