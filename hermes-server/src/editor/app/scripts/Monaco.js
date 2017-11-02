import {ensureServiceIsCreated, retrieveAndUpdateServices} from 'scripts/workers';

export default class Monaco {

  loadEditor(){
    ensureServiceIsCreated(function(selectedService) {
      retrieveAndUpdateServices(selectedService);
      monaco();
    });
  }
} 