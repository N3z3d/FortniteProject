// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

const fs = require('fs');

const resolveChromeBinary = () => {
  const candidates = [
    process.env.CHROME_BIN,
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe',
    'C:\\Program Files (x86)\\BraveSoftware\\Brave-Browser\\Application\\brave.exe'
  ].filter(Boolean);

  return candidates.find(candidate => fs.existsSync(candidate));
};

const chromeBinary = resolveChromeBinary();
if (chromeBinary) {
  process.env.CHROME_BIN = chromeBinary;
}

const hasCliFlag = (flag) => process.argv.some(arg => arg === flag || arg.startsWith(`${flag}=`));
const singleRun = hasCliFlag('--watch=false')
  || hasCliFlag('--single-run')
  || process.env.KARMA_SINGLE_RUN === 'true'
  || process.env.CI === 'true';

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage')
    ],
    client: {
      jasmine: {
        // you can add special configurations here if needed
      },
      clearContext: false // leave Jasmine Spec Runner output visible in browser
    },
    jasmineHtmlReporter: {
      suppressAll: true // removes the duplicated traces
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/frontend'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'json', file: 'coverage-final.json' },
        { type: 'lcovonly', file: 'lcov.info' }
      ]
    },
    reporters: ['progress', 'kjhtml'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: !singleRun,
    browsers: [singleRun ? 'ChromeHeadlessCustom' : 'Chrome'],
    customLaunchers: {
      ChromeHeadlessCustom: {
        base: 'Chrome',
        flags: [
          '--headless',
          '--disable-gpu',
          '--no-sandbox',
          '--disable-dev-shm-usage',
          '--remote-debugging-port=9222'
        ]
      },
      ChromeHeadless: {
        base: 'Chrome',
        flags: [
          '--headless',
          '--disable-gpu',
          '--no-sandbox',
          '--disable-dev-shm-usage',
          '--remote-debugging-port=9222'
        ]
      }
    },
    singleRun,
    restartOnFileChange: !singleRun,
    browserDisconnectTimeout: 10000,
    browserDisconnectTolerance: 3,
    browserNoActivityTimeout: 120000,
    captureTimeout: 300000
  });
}; 
