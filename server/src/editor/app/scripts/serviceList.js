import Helpers from 'scripts/helpers';
import getEndPoint from 'scripts/endPoint'

const helpers = new Helpers();

export function onServiceSelect(e) {
  helpers.setSelectedService(this.textContent);
  helpers.getServiceScript(selectedService, function(script) {
    helpers.setEditorContents(script)
    $('#saveBtn').prop('disabled', true)
  });
  helpers.getJavaHeaders(selectedService, function(script) {
    // TODO: put this stuff somewhere in the UI
  });
  helpers.selectHighlight(e);
  getEndPoint(selectedService);
}


