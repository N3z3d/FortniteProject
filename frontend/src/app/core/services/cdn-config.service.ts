import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * SPRINT 4 - CDN CONFIGURATION SERVICE
 * 
 * Service centralisé pour gestion CDN et optimisation des assets:
 * - Configuration des headers de cache optimaux
 * - Gestion des fallbacks si CDN indisponible
 * - Monitoring des performances CDN
 * - Preloading intelligent des ressources critiques
 * - Service Worker integration pour cache avancé
 */
@Injectable({
  providedIn: 'root'
})
export class CdnConfigService {

  private readonly cdnEnabled: boolean;
  private readonly cdnBaseUrl: string;
  private cdnHealthy: boolean = true;
  
  constructor() {
    this.cdnEnabled = environment.production && environment.cdn?.enabled || false;
    this.cdnBaseUrl = environment.cdn?.baseUrl || '';
    
    if (this.cdnEnabled) {
      this.initializeCdnMonitoring();
      this.preloadCriticalResources();
      this.configureServiceWorker();
    }
  }

  /**
   * Obtient l'URL CDN pour un asset avec fallback automatique
   */
  getAssetUrl(assetPath: string, assetType: 'images' | 'fonts' | 'static' = 'static'): string {
    if (!this.cdnEnabled || !this.cdnHealthy) {
      return this.getLocalAssetUrl(assetPath, assetType);
    }

    const cdnConfig = environment.cdn?.assets?.[assetType];
    if (!cdnConfig?.baseUrl) {
      return this.getLocalAssetUrl(assetPath, assetType);
    }

    return `${cdnConfig.baseUrl}/${assetPath}`;
  }

  /**
   * URL locale en fallback
   */
  private getLocalAssetUrl(assetPath: string, assetType: string): string {
    const basePath = assetType === 'fonts' ? '/assets/fonts' : '/assets';
    return `${basePath}/${assetPath}`;
  }

  /**
   * Configure les headers de cache optimaux pour différents types d'assets
   */
  getCacheHeaders(assetType: 'images' | 'fonts' | 'css' | 'js' | 'html'): Record<string, string> {
    const cacheConfig = environment.cdn?.cache;
    if (!cacheConfig) {
      return {};
    }

    const maxAge = cacheConfig.maxAge?.[assetType] || 3600;
    const headers: Record<string, string> = {
      'Cache-Control': `public, max-age=${maxAge}`,
    };

    // Assets immutables (avec hash dans le nom)
    if (['css', 'js', 'fonts'].includes(assetType)) {
      headers['Cache-Control'] += ', immutable';
    }

    // Assets avec validation
    if (assetType === 'html') {
      headers['Cache-Control'] = 'public, max-age=300, must-revalidate';
      headers['ETag'] = `"${Date.now()}"`;
    }

    return headers;
  }

  /**
   * Précharge les ressources critiques
   */
  private preloadCriticalResources(): void {
    // Précharger les fonts critiques
    const criticalFonts = environment.cdn?.assets?.fonts?.preload || [];
    criticalFonts.forEach((fontFile: string) => {
      this.preloadFont(fontFile);
    });

    // Précharger les images critiques (logos, icônes principales)
    const criticalImages = [
      'logo.webp',
      'icon-192.png',
      'icon-512.png'
    ];
    
    criticalImages.forEach((imagePath: string) => {
      this.preloadImage(imagePath);
    });
  }

  /**
   * Précharge une font avec les bonnes options
   */
  private preloadFont(fontFile: string): void {
    const fontUrl = this.getAssetUrl(fontFile, 'fonts');
    
    const link = document.createElement('link');
    link.rel = 'preload';
    link.as = 'font';
    link.type = 'font/woff2';
    link.crossOrigin = 'anonymous';
    link.href = fontUrl;
    
    // Font display pour améliorer les Core Web Vitals
    if (environment.cdn?.assets?.fonts?.display) {
      (link as any).fontDisplay = environment.cdn.assets.fonts.display;
    }
    
    document.head.appendChild(link);
  }

  /**
   * Précharge une image critique
   */
  private preloadImage(imagePath: string): void {
    const imageUrl = this.getAssetUrl(imagePath, 'images');
    
    const link = document.createElement('link');
    link.rel = 'preload';
    link.as = 'image';
    link.href = imageUrl;
    
    document.head.appendChild(link);
  }

  /**
   * Surveille la santé du CDN
   */
  private initializeCdnMonitoring(): void {
    if (!this.cdnEnabled) return;

    // Test de connectivité CDN toutes les 5 minutes
    setInterval(() => {
      this.checkCdnHealth();
    }, 300000); // 5 minutes

    // Test initial
    this.checkCdnHealth();
  }

  /**
   * Vérifie la disponibilité du CDN
   */
  private async checkCdnHealth(): Promise<void> {
    try {
      const testUrl = `${this.cdnBaseUrl}/health-check.txt`;
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000); // 5s timeout

      const response = await fetch(testUrl, {
        method: 'HEAD',
        signal: controller.signal,
        cache: 'no-cache'
      });

      clearTimeout(timeoutId);
      this.cdnHealthy = response.ok;

      if (!this.cdnHealthy) {
        console.warn('CDN health check failed, falling back to local assets');
      }
    } catch (error) {
      this.cdnHealthy = false;
      console.warn('CDN unreachable, using local assets', error);
    }
  }

  /**
   * Configure le Service Worker pour optimiser le cache
   */
  private configureServiceWorker(): void {
    if (!('serviceWorker' in navigator) || !environment.performance?.serviceWorker?.enabled) {
      return;
    }

    navigator.serviceWorker.register('/sw.js')
      .then(registration => {
        console.log('Service Worker registered successfully');
        
        // Configure les stratégies de cache via le Service Worker
        this.sendCacheConfigToSW(registration);
      })
      .catch(error => {
        console.warn('Service Worker registration failed:', error);
      });
  }

  /**
   * Envoie la configuration de cache au Service Worker
   */
  private sendCacheConfigToSW(registration: ServiceWorkerRegistration): void {
    if (registration.active) {
      const cacheConfig = {
        cdnBaseUrl: this.cdnBaseUrl,
        strategies: environment.performance?.serviceWorker?.strategies || {},
        cacheMaxAge: environment.cdn?.cache?.maxAge || {}
      };

      registration.active.postMessage({
        type: 'CACHE_CONFIG',
        config: cacheConfig
      });
    }
  }

  /**
   * Génère les resource hints pour optimiser le chargement
   */
  generateResourceHints(): void {
    if (!this.cdnEnabled) return;

    // DNS prefetch pour le CDN
    this.addResourceHint('dns-prefetch', this.cdnBaseUrl);
    
    // Preconnect pour établir les connexions
    this.addResourceHint('preconnect', this.cdnBaseUrl, { crossorigin: true });
    
    // Prefetch pour les ressources susceptibles d'être utilisées
    const prefetchAssets = [
      '/static/common.css',
      '/static/icons.svg'
    ];
    
    prefetchAssets.forEach((asset: string) => {
      this.addResourceHint('prefetch', this.getAssetUrl(asset));
    });
  }

  /**
   * Ajoute un resource hint au document
   */
  private addResourceHint(
    rel: string, 
    href: string, 
    attributes: Record<string, any> = {}
  ): void {
    const existingHint = document.querySelector(`link[rel="${rel}"][href="${href}"]`);
    if (existingHint) return; // Évite les doublons

    const link = document.createElement('link');
    link.rel = rel;
    link.href = href;
    
    Object.entries(attributes).forEach(([key, value]) => {
      if (value === true) {
        link.setAttribute(key, '');
      } else if (value) {
        link.setAttribute(key, value);
      }
    });
    
    document.head.appendChild(link);
  }

  /**
   * Métriques CDN pour monitoring
   */
  getCdnMetrics(): CdnMetrics {
    return {
      enabled: this.cdnEnabled,
      healthy: this.cdnHealthy,
      baseUrl: this.cdnBaseUrl,
      cacheStrategy: environment.cdn?.cache?.strategy || 'cache-first'
    };
  }
}

export interface CdnMetrics {
  enabled: boolean;
  healthy: boolean;
  baseUrl: string;
  cacheStrategy: string;
}