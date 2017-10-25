import { getExistingServices, getStubbedServices } from 'scripts/workers';
import Helpers from 'scripts/helpers';
import getEndPoint from 'scripts/endPoint';

const helpers = new Helpers();

export function onServiceSelect(e) {
  if(helpers.getSelectedService() && !document.querySelector('#saveBtn').disabled){
    document.querySelector('.unsaved').textContent = helpers.getSelectedService();
    let modal = document.querySelector('.modal');
    modal.setAttribute('data-service', e.target.textContent);
    modal.classList.toggle('shown');
  } else {
    switchService(e.target.textContent);
  }  
}

export function saveContent(selectedService){
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
  console.log('Save');
}

export function switchService(selectedService){
  helpers.setSelectedService(selectedService);
  helpers.getServiceScript(helpers.getSelectedService(), function(script) {
    helpers.setEditorContents(script)
    $('#saveBtn').prop('disabled', true);
  });
  helpers.getJavaHeaders(helpers.getSelectedService(), function(script) {
    // TODO: put this stuff somewhere in the UI
  });
  helpers.selectHighlight(helpers.getSelectedService());
  getEndPoint(selectedService);
  getExistingServices(selectedService, '.implemented-functions');
  editor.focus();
}


