$(function () {
    'use strict';

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
        var dataTree = $.jstree.reference('#dataTree');
        var objs = dataTree.get_selected(true)
        dataTree.copy(objs);
    });

    $('#cutButton').click(function () {
        var dataTree = $.jstree.reference('#dataTree');
        var objs = dataTree.get_selected(true)
        dataTree.cut(objs);
    });

    $('#createshareButton').click(function () {
        OME.createShare($.jstree.reference('#dataTree').get_selected());
    });

    $('#pasteButton').click(function () {
        var dataTree = $.jstree.reference('#dataTree');
        var objs = dataTree.get_selected(true);
        if (objs.length === 1) {
            dataTree.paste(objs[0]);
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
        var dataTree = $.jstree.reference('#dataTree');
        var selections = dataTree.get_selected();

        $.each(selections, function (index, selection) {
            var path = dataTree.get_path(selection, false, true).reverse();
            var refreshPathReverse = [];
            $.each(path, function (index, pathComponent) {
                var node = dataTree.get_node(pathComponent);
                var tuple = [node.type, node.data.obj.id];
                refreshPathReverse.push(tuple);
            });
            refreshPathsReverse.push(refreshPathReverse);
        });

        dataTree.deselect_all();
        // NB: the global variable refreshPathsReverse is used in ome.tree.js
        // after refresh, then set to empty list.
        dataTree.refresh();
    });


    // We (un)truncate images when the left panel resizes...
    $("#left_panel").on('resize', function (event) {
        var dataTree = $.jstree.reference('#dataTree');
        dataTree.redraw(true);
    });

    // Handle creation of new Project, Dataset or Screen...
    $("#new-container-form").dialog({
        autoOpen: false,
        resizable: true,
        height: 280,
        width: 420,
        modal: true,
        buttons: {
            "OK": function () {
                createNewContainer();
                $(this).dialog("close");
            },
            "Cancel": function () {
                $(this).dialog("close");
            }
        }
    });

    // same code is called from closing dialog or 'submit' of form
    $("#new-container-form").submit(function () {
        $("#new-container-form").dialog("close");
        createNewContainer();
        return false;
    });

    $("#delete-dialog-form").dialog({
        dialogClass: 'delete_confirm_dialog',
        autoOpen: false,
        resizable: true,
        height: 210,
        width: 420,
        modal: true,
        buttons: {
            "Yes": function () {
                $("#delete-dialog-form").data("clicked_button", "Yes");
                $(this).dialog("close");
            },
            "No": function () {
                $("#delete-dialog-form").data("clicked_button", "No");
                $(this).dialog("close");
            }
        }
    });

    var createNewContainer = function () {
        var cont_type = $("#new_container_type").text().toLowerCase();  // E.g. 'project'
        var $f = $("#new-container-form");
        var new_container_name = $("input[name='name']", $f).val();
        var new_container_desc = $("textarea[name='description']", $f).val();
        var new_container_owner = $("input[name='owner']", $f).val();
        if ($.trim(new_container_name).length === 0) {
            alert("Please enter a Name");
            return;
        }

        // If images under orphaned are selected, note IDs (for adding to new dataset)
        var dataTree = $.jstree.reference('#dataTree');
        var selected = dataTree.get_selected(true);
        // TODO Only keeping img_ids because it is simpler to POST the data using that
        // Can be removed when updating the ajax call
        var img_ids = [];
        var orphaned_image_nodes = [];

        $.each(selected, function (index, node) {
            if (node.type === 'image' &&
                dataTree.get_node(dataTree.get_parent(node)).type === 'orphaned' &&
                OME.nodeHasPermission(node, 'canLink')) {
                img_ids.push(node.data.obj.id);
                orphaned_image_nodes.push(node);
            }
        });

        // Default: Create an orphan of "folder_type" ('project', 'dataset', 'screen', 'tag', 'tagset' etc. )
        var url = window.urls._manage_action_containers._add_new_container;
        // Find the 'experimenter' node as parent
        var root = dataTree.get_node('#');

        $.each(root.children, function (index, id) {
            var node = dataTree.get_node(id);
            if (node.type === 'experimenter' && node.data.obj.id === window.user_id)
            {
                parent = node;
                // Break out of each
                return false;
            }
        });

        // If a position project is selected (or selected is a child of project) create dataset under it
        var parent = false;
        if (selected.length > 0 && cont_type === 'dataset') {
            if (selected[0].type === 'project') {
                parent = selected[0];
            } else if (dataTree.get_node(selected[0].parent).type === 'project') {
                parent = dataTree.get_node(selected[0].parent);
            }
            // If a tagset is selected (or selected is a child of tagset), create tag under it
        } else if (selected.length > 0 && cont_type === 'tag') {
            if (selected[0].type === 'tagset') {
                parent = selected[0];
            } else if (dataTree.get_node(selected[0].parent).type === 'tagset') {
                parent = dataTree.get_node(selected[0].parent);
            }
        }
        if (parent) {
            url = url + parent.type + '/' + parent.data.obj.id + '/';
        } else {
            // otherwise create an orphan of "folder_type" ('project', 'dataset', 'screen', 'tag', 'tagset' etc. )
            // Find 'experimenter' to be parent
            $.each(root.children, function (index, id) {
                var node = dataTree.get_node(id);
                if (node.type === 'experimenter' && node.data.obj.id === window.user_id)
                {
                    parent = node;
                    // Break out of each
                    return false;
                }
            });
        }

        var ajax_data = {
            "name": new_container_name,
            "folder_type": cont_type,
            "description": new_container_desc,
            "owner": new_container_owner
        };

        if (img_ids.length > 0) {
            ajax_data['image'] = img_ids;
        }

        $.ajax({
            url: url,
            data: ajax_data,
            dataType: "json",
            type: "POST",
            traditional: true,
            success: function (r) {

                var data = {
                    'id': r['id'],
                    'isOwner': true,
                    'ownerId': window.user_id,
                    'name':
                    new_container_name,
                    'permsCss':
                        'canEdit canAnnotate canLink canDelete canChgrp'
                };

                var node = {
                    'data': {'id': r['id'], 'obj': data},
                    'text': new_container_name,
                    'children': false,
                    'type': cont_type,
                    'li_attr': {
                        'class': cont_type,
                        'data-id': r['id']
                    }
                };

                // Create the node, move any orphans into it and select only it
                node = JSON.parse(JSON.stringify(node));
                dataTree.create_node(parent, node, 'last', function (node) {
                    if (orphaned_image_nodes.length > 0) {
                        dataTree.move_node(orphaned_image_nodes, node);
                    }
                    // There is no need to update duplicates at the moment as nothing that
                    // can be created could have a duplicate to need updating
                    dataTree.deselect_all();
                    dataTree.select_node(node);
                    //TODO Scroll to new if off screen? https://github.com/vakata/jstree/issues/519
                });
            }
        });
    };



});
