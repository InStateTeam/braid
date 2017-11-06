import {switchService} from 'scripts/workers';
import Helpers from 'scripts/helpers';

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