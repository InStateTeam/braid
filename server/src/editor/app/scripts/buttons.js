import getEndPoint from 'scripts/endPoint';
import { getServiceScript, retrieveAndUpdateServices } from 'scripts/workers';

import Helpers from 'scripts/helpers'

const helpers = new Helpers();

export default class Buttons {

  onCreateService() {
    const serviceName = $('#newService').val();
    getServiceScript(serviceName, function() {
      retrieveAndUpdateServices(serviceName);
      $('#newService').val("");
    });
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