import getEndPoint from 'scripts/endPoint';
import Buttons from 'scripts/buttons';
import Monaco from 'scripts/Monaco';
import EventListeners from 'scripts/eventListeners';

export default class App {
  constructor(){
    $.delete = function(url, callback){
      return $.ajax({
        url: url,
        method: 'DELETE',
        success: callback
      });
    }
    this.Buttons = new Buttons();
    this.Monaco = new Monaco();
  }
  
  init(){
    getEndPoint('');
    this.bindEvents();
    this.Monaco.loadEditor();
    this.EventListeners = new EventListeners(); 
    $('#newService').focus();
  }

  bindEvents() {
    $('#saveBtn').click(this.Buttons.onSave);
    $('#formatBtn').click(this.Buttons.onFormat);
    $('#createServiceBtn').click(this.Buttons.onCreateService);
    $('#deleteBtn').click(this.Buttons.onDelete);
  }
}

