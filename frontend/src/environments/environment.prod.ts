export const environment = {
  production: true,
  apiUrl: 'http://localhost:8081',
  apiBaseUrl: 'http://localhost:8081',
  wsUrl: 'ws://localhost:8081/ws',
  enableFallbackData: false,
  defaultDevUser: null,

  // CDN configuration (basic production setup)
  cdn: {
    enabled: false, // Enable when CDN is configured
    baseUrl: '',
    assets: {
      images: {
        baseUrl: '/assets/images',
        formats: ['webp', 'jpg', 'png'],
        sizes: [150, 300, 600, 1200],
        lazyLoading: true,
        placeholder: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMSIgaGVpZ2h0PSIxIiB2aWV3Qm94PSIwIDAgMSAxIiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9IiNGNUY1RjUiLz48L3N2Zz4='
      },
      fonts: {
        baseUrl: '/assets/fonts',
        preload: [
          'Roboto-Regular.woff2',
          'Roboto-Medium.woff2'
        ],
        display: 'swap'
      },
      static: {
        baseUrl: '/assets',
        cacheMaxAge: 31536000,
        immutable: true
      }
    },
    cache: {
      strategy: 'cache-first',
      maxAge: {
        images: 2592000,
        fonts: 31536000,
        css: 31536000,
        js: 31536000,
        html: 300
      }
    }
  },

  // Performance configuration (production optimizations)
  performance: {
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
    lazyLoading: {
      enabled: true,
      preloadStrategy: 'selective',
      intersectionThreshold: 0.1,
      rootMargin: '50px'
    },
    serviceWorker: {
      enabled: true,
      scope: '/',
      strategies: {
        images: 'cache-first',
        api: 'network-first',
        static: 'cache-first'
      }
    }
  }
};
