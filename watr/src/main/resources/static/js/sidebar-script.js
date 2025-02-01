/*!
    * Start Bootstrap - SB Admin v7.0.7 (https://startbootstrap.com/template/sb-admin)
    * Copyright 2013-2023 Start Bootstrap
    * Licensed under MIT (https://github.com/StartBootstrap/startbootstrap-sb-admin/blob/master/LICENSE)
    */
//
// Scripts
//

window.addEventListener('DOMContentLoaded', event => {

    // Toggle the side navigation
    const sidebarToggle = document.body.querySelector('#sidebarToggle');
    if (sidebarToggle) {
        // Uncomment Below to persist sidebar toggle between refreshes
        // if (localStorage.getItem('sb|sidebar-toggle') === 'true') {
        //     document.body.classList.toggle('sb-sidenav-toggled');
        // }
        sidebarToggle.addEventListener('click', event => {
            event.preventDefault();
            document.body.classList.toggle('sb-sidenav-toggled');
            localStorage.setItem('sb|sidebar-toggle', document.body.classList.contains('sb-sidenav-toggled'));
        });
    }

    function setActiveButton(target) {
        document.querySelectorAll(".nav-link").forEach(btn => btn.classList.remove("active"));
        let activeButton = document.querySelector(`.nav-link[data-target="${target}"]`);
        if (activeButton) {
            activeButton.classList.add("active");
        }
    }

    document.body.addEventListener("htmx:afterSwap", function (event) {
        let target = event.detail.target.id;
        setActiveButton(target);
    });

    window.openUploadSection = function () {
        let datasetsSection = document.getElementById('collapseLayoutsDatasets');
        if (datasetsSection && !datasetsSection.classList.contains('show')) {
            document.querySelector('[data-bs-target="#collapseLayoutsDatasets"]').click();
        }
        setTimeout(() => {
            document.getElementById("uploadBtn").click();
        }, 300);
    };

});
