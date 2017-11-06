import Buttons from 'scripts/buttons';
import Monaco from 'scripts/Monaco';
import EventListeners from 'scripts/eventListeners';

export default class App {
  constructor(){
    this.Monaco = new Monaco();
  }
  
  init(){
    this.Monaco.loadEditor();
    this.EventListeners = new EventListeners(); 
  }
}

