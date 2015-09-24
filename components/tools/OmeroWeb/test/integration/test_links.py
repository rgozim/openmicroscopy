#!/usr/bin/env python
# -*- coding: utf-8 -*-

# Copyright (C) 2015 University of Dundee & Open Microscopy Environment.
# All rights reserved.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

"""
Tests creation and deletion of links between e.g. Projects & Datasets etc.
"""

import omero

from omero.rtypes import rstring
from weblibrary import IWebTest
from weblibrary import _csrf_post_response, _get_response
from weblibrary import _csrf_delete_response

import json

from django.core.urlresolvers import reverse

import pytest


class TestLinks(IWebTest):
    """
    Tests creation and deletion of links between
    e.g. Projects & Datasets etc.
    """

    @pytest.fixture
    def project(self):
        """Returns a new OMERO Project with required fields set."""
        project = omero.model.ProjectI()
        project.name = rstring(self.uuid())
        return self.update.saveAndReturnObject(project)

    @pytest.fixture
    def dataset(self):
        """Returns a new OMERO Dataset with required fields set."""
        dataset = omero.model.DatasetI()
        dataset.name = rstring(self.uuid())
        return self.update.saveAndReturnObject(dataset)

    @pytest.fixture
    def datasets(self):
        """Returns 2 new OMERO Datasets with required fields set."""
        dataset = omero.model.DatasetI()
        dataset.name = rstring("A_%s" % self.uuid())
        dataset2 = omero.model.DatasetI()
        dataset2.name = rstring("B_%s" % self.uuid())
        return self.update.saveAndReturnArray([dataset, dataset2])

    @pytest.fixture
    def images(self):
        image = self.new_image(name="A_%s" % self.uuid())
        image2 = self.new_image(name="B_%s" % self.uuid())
        return self.update.saveAndReturnArray([image, image2])

    @pytest.fixture
    def screens(self):
        """Returns 2 new OMERO Screens with required fields set."""
        screen = omero.model.ScreenI()
        screen.name = rstring("A_%s" % self.uuid())
        screen2 = omero.model.ScreenI()
        screen2.name = rstring("B_%s" % self.uuid())
        return self.update.saveAndReturnArray([screen, screen2])

    @pytest.fixture
    def plates(self):
        """Returns 2 new OMERO Plates with required fields set."""
        plate = omero.model.PlateI()
        plate.name = rstring("A_%s" % self.uuid())
        plate2 = omero.model.PlateI()
        plate2.name = rstring("B_%s" % self.uuid())
        return self.update.saveAndReturnArray([plate, plate2])

    def test_link_project_datasets(self, project, datasets):
        # Link Project to Datasets
        request_url = reverse("api_links")
        pid = project.id.val
        dids = [d.id.val for d in datasets]
        data = {
            'project': {pid: {'dataset': dids}}
        }
        rsp = _csrf_post_response_json(self.django_client, request_url, data)
        assert rsp == {"success": True}

        # Check links
        request_url = reverse("api_datasets")
        rsp = _get_response_json(self.django_client, request_url, {'id': pid})
        # Expect a single Dataset with correct id
        assert len(rsp['datasets']) == 2
        assert rsp['datasets'][0]['id'] == dids[0]
        assert rsp['datasets'][1]['id'] == dids[1]

    def test_link_datasets_images(self, datasets, images):
        # Link Datasets to Images
        request_url = reverse("api_links")
        dids = [d.id.val for d in datasets]
        iids = [i.id.val for i in images]
        # Link first dataset to first image,
        # Second dataset linked to both images
        data = {
            'dataset': {dids[0]: {'image': [iids[0]]},
                        dids[1]: {'image': iids}}
        }
        rsp = _csrf_post_response_json(self.django_client, request_url, data)
        assert rsp == {"success": True}

        # Check links
        request_url = reverse("api_images")
        # First Dataset has single image
        rsp = _get_response_json(self.django_client,
                                 request_url, {'id': dids[0]})
        assert len(rsp['images']) == 1
        assert rsp['images'][0]['id'] == iids[0]
        # Second Dataset has both images
        rsp = _get_response_json(self.django_client,
                                 request_url, {'id': dids[1]})
        assert len(rsp['images']) == 2
        assert rsp['images'][0]['id'] == iids[0]
        assert rsp['images'][1]['id'] == iids[1]

    def test_unlink_screen_plate(self, screens, plates):
        # Link both plates to both screens
        request_url = reverse("api_links")
        sids = [s.id.val for s in screens]
        pids = [p.id.val for p in plates]
        # Link first dataset to first image,
        # Second dataset linked to both images
        data = {
            'screen': {sids[0]: {'plate': pids},
                       sids[1]: {'plate': pids}}
        }
        rsp = _csrf_post_response_json(self.django_client, request_url, data)
        assert rsp == {"success": True}

        # Unlink first Plate from first Screen
        request_url = reverse("api_links")
        data = {
            'screen': {sids[0]: {'plate': pids[:1]}}
        }
        response = _csrf_delete_response(self.django_client,
                                         request_url,
                                         json.dumps(data),
                                         content_type="application/json")
        # Returns remaing link from first Plate to 2nd Screen
        response = json.loads(response.content)
        assert response == {"success": True,
                            "screen": {str(sids[1]): {"plate": pids[:1]}}
                            }


def _get_response_json(django_client, request_url, query_string):
    rsp = _get_response(django_client, request_url,
                        query_string, status_code=200)
    return json.loads(rsp.content)


def _csrf_post_response_json(django_client, request_url, data):
    rsp = _csrf_post_response(django_client,
                              request_url,
                              json.dumps(data),
                              content_type="application/json")
    return json.loads(rsp.content)
