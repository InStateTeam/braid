import Helpers from 'scripts/helpers';

const helpers = new Helpers();

export function ensureServiceIsCreated(callback) {
  const params = helpers.parseURL(document.URL).searchObject;
  if (typeof(params['service']) !== 'undefined' && params.service !== null) {
    helpers.getServiceScript(params.service, function() {
      callback(params.service);
    })
  } else {
    callback(null);
  }
}

export function retrieveAndUpdateServices(selectedService) {
  $.get("/api/services", function (data) {
    updateServices(data, selectedService);
  });
}

export function updateServices(services, selectedService) {
  const selection = $('#services');
  populateServiceOptions(selection, services);
  if (selectedService) {
    selection.val(selectedService).trigger('change');
    switchService(selectedService);
  }
}

export function switchService(selectedService){
  helpers.setSelectedService(selectedService);
  helpers.getServiceScript(selectedService, function(script) {
    helpers.setEditorContents(script)
    $('#saveBtn').prop('disabled', true)
  });
  helpers.getJavaHeaders(selectedService, helpers.showExistingServices);
  helpers.selectHighlight(helpers.getSelectedService());
  helpers.getEndPoint(selectedService);
  editor.focus();
}

function populateServiceOptions(selection, services) {
  selection.empty();

  for (var idx in services) {
    const service = services[idx];
    const option = $('<li>', {value: service, text: service})
    //option.click(onServiceSelect);
    selection.append(option);
  }
}