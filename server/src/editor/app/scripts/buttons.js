import getEndPoint from 'scripts/endPoint';
import { getServiceScript, retrieveAndUpdateServices } from 'scripts/workers';

import Helpers from 'scripts/helpers'

const helpers = new Helpers();

export default class Buttons {

  onCreateService() {
    const serviceName = $('#newService').val();
    const tooltip = document.querySelector('.tooltip');
    const isValidServiceName = helpers.checkCreatedService(serviceName);
    if(isValidServiceName){
      getServiceScript(serviceName, function() {
        retrieveAndUpdateServices(serviceName);
        $('#newService').val("");
        tooltip.classList.remove('shown');
      });
    } else {
      let invalidServiceName = helpers.parseCreateService(serviceName);
      document.querySelector('.badService').innerHTML = invalidServiceName.highlightedString;
  
      (invalidServiceName.empty) ? 
      document.querySelector('.empty').classList.add('shown') : document.querySelector('.empty').classList.remove('shown');
  
      (invalidServiceName.first) ? 
      document.querySelector('.first').classList.add('shown') : document.querySelector('.first').classList.remove('shown');
  
      (invalidServiceName.remain) ? 
      document.querySelector('.remain').classList.add('shown') : document.querySelector('.remain').classList.remove('shown');    
  
      tooltip.classList.add('shown');
    }
 }

 onSave(e) {
  const script = editor.getValue();
  const service = helpers.getSelectedService();

  $.post("/api/services/" + service + "/script", script)
    .done(function() {
      console.log("saved")
      $('#saveBtn').prop('disabled', true)
    })
    .fail(function(e) {
      console.log("failed to save", e);
    })
  }

  onFormat(e) {
    editor.trigger('', 'editor.action.formatDocument');
  }

  onDelete(e){
    const serviceName = helpers.getSelectedService();
    const callValue = "/api/services/" + serviceName;
    $.delete(callValue, function(){
      helpers.setSelectedService('');
      retrieveAndUpdateServices('');
    });
    getEndPoint('');
  }
}