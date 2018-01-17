import App from 'scripts/app';

const app = new App();

document.addEventListener('DOMContentLoaded', () => {
  app.init();  
  console.log('Initialized app');
});
