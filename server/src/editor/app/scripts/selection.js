export default function myListeners(){
  
  document.querySelector('#services').addEventListener('click', (e) => {
    if(e.target.tagName === 'LI'){
      const lists = document.querySelector('#services').querySelectorAll('li');
      for(let item = 0; item < lists.length; item++){
        lists[item].style.background = '#1973E2';
        //item.style.background = "#"
      }
      e.target.style.background = "#0E3A70"
    }
  });
}