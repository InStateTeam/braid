//import 'babel-polyfill';
//import AnApp from 'scripts/app';
import ReactDOM from 'react-dom';
import React from 'react';
import App from 'components/App/App';

document.addEventListener('DOMContentLoaded', () => {
  // do your setup here

  ReactDOM.render(<App />, document.querySelector('#app'));
  //AnApp();
  console.log('Initialized app');
});
