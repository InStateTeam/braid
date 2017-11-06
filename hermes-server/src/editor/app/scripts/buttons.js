import {retrieveAndUpdateServices} from 'scripts/workers';

import Helpers from 'scripts/helpers'

const helpers = new Helpers();

export default class Buttons {
  constructor(){
    this.onCreateService = this.onCreateService.bind(this);
  }

  onCreateService() {
    const serviceName = $('#newService').val();
    const tooltip = document.querySelector('.tooltip');
    const isValidServiceName = helpers.checkCreatedService(serviceName);
    if(isValidServiceName){
      helpers.getServiceScript(serviceName, function() {
        retrieveAndUpdateServices(serviceName);
        $('#newService').val("");
        tooltip.classList.remove('shown');
      });
    } else {
      this.formatTooltip(serviceName)
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

  onDelete(e) {
    const serviceName = helpers.getSelectedService();
    const callValue = "/api/services/" + serviceName;
    $.ajax({
      url: callValue,
      method: 'DELETE',
      success:  () => {
        helpers.setSelectedService('');
        retrieveAndUpdateServices('');
      }
    });
  }

  formatTooltip(serviceName) {
    let invalidServiceName = helpers.parseCreateService(serviceName);
    document.querySelector('.badService').innerHTML = invalidServiceName.highlightedString;

    (invalidServiceName.empty) ? 
    document.querySelector('.empty').classList.add('shown') : document.querySelector('.empty').classList.remove('shown');

    (invalidServiceName.first) ? 
    document.querySelector('.first').classList.add('shown') : document.querySelector('.first').classList.remove('shown');

    (invalidServiceName.remain) ? 
    document.querySelector('.remain').classList.add('shown') : document.querySelector('.remain').classList.remove('shown');    

    document.querySelector('.tooltip').classList.add('shown');
  }
}