import getEndPoint from 'scripts/endPoint';
import Buttons from 'scripts/buttons';
import Monaco from 'scripts/Monaco';
import Helpers from 'scripts/helpers';

export default class App {
  constructor(){
    $.delete = function(url, callback){
      return $.ajax({
        url: url,
        method: 'DELETE',
        success: callback
      });
    }
    const helpers = new Helpers(); 
    this.Buttons = new Buttons();
    this.Monaco = new Monaco();
  }
  
  init(){
    getEndPoint('');
    this.bindEvents();
    this.Monaco.loadEditor();
  }

  bindEvents() {
    $('#saveBtn').click(this.Buttons.onSave);
    $('#formatBtn').click(this.Buttons.onFormat);
    $('#createServiceBtn').click(this.Buttons.onCreateService);
    $('#deleteBtn').click(this.Buttons.onDelete);
  }
}

