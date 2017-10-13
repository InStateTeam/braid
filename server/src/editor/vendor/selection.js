function selectHighlight(e){
  const lists = document.querySelector('#services').querySelectorAll('li');
  for(let item = 0; item < lists.length; item++){
    lists[item].style.background = '#1973E2';
  }
  e.target.style.background = "#0E3A70"
}

function highlightNewService(selectedService){
  const lists = document.querySelector('#services').querySelectorAll('li');
  console.log('this ran');
  let newServiceIndex = "";
  for(let item = 0; item < lists.length; item++){
    lists[item].style.background = '#1973E2';
    if(lists[item].innerText == selectedService){
      newServiceIndex = item;
    }
  }

  if(newServiceIndex){
    lists[newServiceIndex].style.background = "#0E3A70";
  }
}

function initHighlight(selectedService){
  const item = document.querySelector('#services').querySelector('li');
  console.log(item);
}