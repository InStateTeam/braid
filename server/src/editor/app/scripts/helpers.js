export default class Helpers {
  
  getSelectedService() {
    return selectedService;
  }    

  setSelectedService(service) {
    selectedService = service;
  }

  setEditorContents(script) {
    editor.setValue(script)
  }

  getServiceScript(serviceName, callback) {
    $.get("/api/services/" + serviceName + "/script", function(script) {
      callback(script)
    })
  }

  getJavaHeaders(serviceName, callback) {
    $.get("/api/services/" + serviceName + "/java", function(script) {
      callback(script);
    });
  }
  
  parseURL(url) {
    var parser = document.createElement('a'),
      searchObject = {},
      queries, split, i;
    // Let the browser do the work
    parser.href = url;
    // Convert query string to object
    queries = parser.search.replace(/^\?/, '').split('&');
    for( i = 0; i < queries.length; i++ ) {
      split = queries[i].split('=');
      searchObject[split[0]] = split[1];
    }
    return {
      protocol: parser.protocol,
      host: parser.host,
      hostname: parser.hostname,
      port: parser.port,
      pathname: parser.pathname,
      search: parser.search,
      searchObject: searchObject,
      hash: parser.hash
    };
  }
  
  selectHighlight(e){
    const lists = document.querySelector('#services').querySelectorAll('li');
    for (let item = 0; item < lists.length; item++) {
      lists[item].style.background = '#000';
    }
    e.target.style.background = "#EF0017"
  }

  highlightNewService(selectedService){
    const lists = document.querySelector('#services').querySelectorAll('li');
    let newServiceIndex = "";
    for (let item = 0; item < lists.length;  item++) {
      lists[item].style.background = '#000';
      if(lists[item].innerText === selectedService){
        newServiceIndex = item;
      }
    }
    if(newServiceIndex){
      lists[newServiceIndex].style.background = "#EF0017";
    }
  }

  populateFunctions(serviceArray){
    const functionList = document.querySelector('.implemented-functions');
    while(functionList.firstChild){
      functionList.removeChild(functionList.firstChild);
    }
    serviceArray.map((service) => {
      const node = document.createElement("LI");
      // TODO: Chris you may wish to style this properly :-)
      const text = JSON.stringify(service);
      const textNode = document.createTextNode(text);
      node.appendChild(textNode);
      functionList.appendChild(node);
    });
  }
}

  