import { onServiceSelect } from 'scripts/serviceList';
import Helpers from 'scripts/helpers';
import Monaco from 'scripts/Monaco';

const helpers = new Helpers();

export function getServiceScript(serviceName, callback) {
  $.get("/api/services/" + serviceName + "/script", function(script) {
    callback(script);
  });
}

export function retrieveAndUpdateServices(selectedService) {
  $.get("/api/services", function (data) {
    updateServices(data, selectedService);
  });
}

export function ensureServiceIsCreated(callback) {
  const params = helpers.parseURL(document.URL).searchObject;
  if (typeof(params['service']) !== 'undefined' && params.service !== null) {
    helpers.getServiceScript(params.service, function() {
      callback(params.service)
    })
  } else {
    callback(null)
  }
}

export function updateServices(services, selectedService) {
  const selection = $('#services');
  populateServiceOptions(selection, services);
  if (!selectedService && services.length > 0) {
    selectedService = services[0];
  }
  else if (selectedService) {
    selection.val(selectedService).trigger('change');
    helpers.setSelectedService(selectedService);
    helpers.getServiceScript(selectedService, function(script) {
      helpers.setEditorContents(script)
      $('#saveBtn').prop('disabled', true)
    });
    helpers.getJavaHeaders(selectedService, function(script) {
      // TODO: put this stuff somewhere in the UI
    });
    helpers.highlightNewService(selectedService);
   }
}

export function populateServiceOptions(selection, services) {
  selection.empty();

  for (var idx in services) {
    const service = services[idx];
    const option = $('<li>', {value: service, text: service})
    option.click(onServiceSelect);
    selection.append(option);
  }
}

export function getExistingServices(serviceName){
  $.get("/api/services/" + serviceName + "/java", function(data) {
    
    console.log(data);
    const strArray = data.split("\n\n");
    helpers.populateFunctions(strArray);
    helpers.expandFunctionsSection();
  });
}

export function getStubbedServices(serviceName){
  $.get("/api/services/" + serviceName + "/script", function(data) {
    // TODO: parse return type here
 });
}

