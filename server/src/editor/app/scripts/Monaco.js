import { getServiceScript, 
         retrieveAndUpdateServices,
         ensureServiceIsCreated
        } from 'scripts/workers';

export default class Monaco {

  loadEditor(){
    ensureServiceIsCreated(function(selectedService) {
      retrieveAndUpdateServices(selectedService);
      monaco();
  });
  }
} 