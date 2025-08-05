/**
 * SPRINT 4 - CDN ENVIRONMENT CONFIGURATION
 * 
 * Configuration pour déploiement avec CDN optimisé pour 500+ utilisateurs
 * - Static assets servies depuis CDN global
 * - Images optimisées avec WebP et fallbacks
 * - Fonts pré-chargées depuis CDN
 * - Cache headers optimisés pour performance
 */

export const environment = {
  production: true,
  apiBaseUrl: 'https://api.fortnite-pronos.com',
  
  // CDN Configuration pour performance globale
  cdn: {
    enabled: true,
    baseUrl: 'https://cdn.fortnite-pronos.com',
    
    // Assets optimization
    assets: {
      images: {
        baseUrl: 'https://cdn.fortnite-pronos.com/images',
        formats: ['webp', 'jpg', 'png'], // WebP first, fallback to JPG/PNG
        sizes: [150, 300, 600, 1200], // Responsive image sizes
        lazyLoading: true,
        placeholder: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMSIgaGVpZ2h0PSIxIiB2aWV3Qm94PSIwIDAgMSAxIiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9IiNGNUY1RjUiLz48L3N2Zz4='
      },
      
      fonts: {
        baseUrl: 'https://cdn.fortnite-pronos.com/fonts',
        preload: [
          'Roboto-Regular.woff2',
          'Roboto-Medium.woff2',
          'Roboto-Bold.woff2'
        ],
        display: 'swap' // Améliore les Core Web Vitals
      },
      
      static: {
        baseUrl: 'https://cdn.fortnite-pronos.com/static',
        cacheMaxAge: 31536000, // 1 year cache
        immutable: true
      }
    },
    
    // Cache configuration
    cache: {
      strategy: 'cache-first',
      maxAge: {
        images: 2592000,    // 30 days
        fonts: 31536000,    // 1 year  
        css: 31536000,      // 1 year (avec versioning)
        js: 31536000,       // 1 year (avec versioning)
        html: 300           // 5 minutes
      }
    }
  },
  
  // Performance optimizations
  performance: {
    // Bundle splitting optimization
    bundleOptimization: {
      vendorChunk: true,
      commonChunk: true,
      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          vendor: {
            test: /[\\/]node_modules[\\/]/,
            name: 'vendors',
            chunks: 'all',
            priority: 20
          },
          common: {
            name: 'common',
            minChunks: 2,
            chunks: 'all',
            priority: 10,
            reuseExistingChunk: true,
            enforce: true
          }
        }
      }
    },
    
    // Lazy loading configuration
    lazyLoading: {
      enabled: true,
      preloadStrategy: 'selective', // Preload critical routes only
      intersectionThreshold: 0.1,
      rootMargin: '50px'
    },
    
    // Service Worker configuration
    serviceWorker: {
      enabled: true,
      scope: '/',
      strategies: {
        images: 'cache-first',
        api: 'network-first',
        static: 'cache-first'
      }
    }
  },
  
  // Monitoring et analytics
  monitoring: {
    webVitals: {
      enabled: true,
      thresholds: {
        LCP: 2500,  // Largest Contentful Paint (ms)
        FID: 100,   // First Input Delay (ms)
        CLS: 0.1    // Cumulative Layout Shift
      }
    },
    
    analytics: {
      enabled: true,
      trackPageViews: true,
      trackUserInteractions: true,
      trackPerformanceMetrics: true
    }
  },
  
  // Feature flags pour A/B testing
  features: {
    progressiveImageLoading: true,
    virtualScrolling: true,
    advancedCaching: true,
    compressionOptimization: true,
    criticalResourceHints: true
  }
};