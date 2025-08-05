export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  apiBaseUrl: 'http://localhost:8080',
  wsUrl: 'ws://localhost:8080/ws',
  enableFallbackData: false,

  // CDN configuration (disabled in development)
  cdn: {
    enabled: false,
    baseUrl: '',
    assets: {
      images: {
        baseUrl: '/assets/images',
        formats: ['jpg', 'png', 'svg'],
        sizes: [150, 300, 600, 1200],
        lazyLoading: true,
        placeholder: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMSIgaGVpZ2h0PSIxIiB2aWV3Qm94PSIwIDAgMSAxIiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9IiNGNUY1RjUiLz48L3N2Zz4='
      },
      fonts: {
        baseUrl: '/assets/fonts',
        preload: [],
        display: 'swap'
      },
      static: {
        baseUrl: '/assets',
        cacheMaxAge: 3600,
        immutable: false
      }
    },
    cache: {
      strategy: 'cache-first',
      maxAge: {
        images: 3600,
        fonts: 86400,
        css: 86400,
        js: 86400,
        html: 300
      }
    }
  },

  // Performance configuration (basic settings for development)
  performance: {
    bundleOptimization: {
      vendorChunk: false,
      commonChunk: false,
      splitChunks: {
        chunks: 'async'
      }
    },
    lazyLoading: {
      enabled: true,
      preloadStrategy: 'none',
      intersectionThreshold: 0.1,
      rootMargin: '50px'
    },
    serviceWorker: {
      enabled: false,
      scope: '/',
      strategies: {
        images: 'network-first',
        api: 'network-first',
        static: 'network-first'
      }
    }
  }
}; 