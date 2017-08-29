$(function () {
    'use strict';


    var inst = $.jstree.reference('#dataTree');

    // Attach click handlers to the individual buttons

    $('#addprojectButton').click(function () {
        OME.handleNewContainer("project");
    });

    $('#adddatasetButton').click(function () {
        OME.handleNewContainer("dataset");
    });

    $('#addscreenButton').click(function () {
        OME.handleNewContainer("screen");
    });

    $('#addtagButton').click(function () {
        OME.handleNewContainer("tag");
    });

    $('#addtagsetButton').click(function () {
        OME.handleNewContainer("tagset");
    });

    $('#copyButton').click(function () {
        var objs = inst.get_selected(true)
        inst.copy(objs);
    });

    $('#cutButton').click(function () {
        var objs = inst.get_selected(true)
        inst.cut(objs);
    });

    $('#createshareButton').click(function () {
        OME.createShare(inst.get_selected());
    });

    $('#pasteButton').click(function () {
        var objs = inst.get_selected(true);
        if (objs.length == 1) {
            inst.paste(objs[0]);
            // Always disable paste button immediatly after using it
            enableToolbarButton('paste', false);
        }
    });

    $('#deleteButton').click(function () {
        var userId = window.user_id;
        var deleteUrl = window.urls._manage_action_containers._delete_many;
        var filesetCheckUrl = window.urls._fileset_check._delete;
        OME.handleDelete(deleteUrl, filesetCheckUrl, userId);
    });

    $('#refreshButton').click(function () {
        // Grab the paths to the items that are currently selected, for restoration later
        var selections = inst.get_selected();

        $.each(selections, function (index, selection) {
            var path = inst.get_path(selection, false, true).reverse();
            var refreshPathReverse = [];
            $.each(path, function (index, pathComponent) {
                var node = inst.get_node(pathComponent);
                var tuple = [node.type, node.data.obj.id];
                refreshPathReverse.push(tuple);
            });
            refreshPathsReverse.push(refreshPathReverse);
        });

        inst.deselect_all();
        // NB: the global variable refreshPathsReverse is used in ome.tree.js
        // after refresh, then set to empty list.
        inst.refresh();
    });


});
