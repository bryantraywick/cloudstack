# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
from marvin.integration.lib.base import CloudStackEntity
from marvin.cloudstackAPI import suspendProject
from marvin.cloudstackAPI import createProject
from marvin.cloudstackAPI import listProjects
from marvin.cloudstackAPI import updateProject
from marvin.cloudstackAPI import activateProject
from marvin.cloudstackAPI import deleteProject
from marvin.cloudstackAPI import deleteAccountFromProject
from marvin.cloudstackAPI import addAccountToProject
from marvin.cloudstackAPI import listProjectAccounts

class Project(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def suspend(self, apiclient, **kwargs):
        cmd = suspendProject.suspendProjectCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        project = apiclient.suspendProject(cmd)
        return project


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createProject.createProjectCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        project = apiclient.createProject(cmd)
        return Project(project.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listProjects.listProjectsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        project = apiclient.listProjects(cmd)
        return map(lambda e: Project(e.__dict__), project)


    def update(self, apiclient, **kwargs):
        cmd = updateProject.updateProjectCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        project = apiclient.updateProject(cmd)
        return project


    def activate(self, apiclient, **kwargs):
        cmd = activateProject.activateProjectCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        project = apiclient.activateProject(cmd)
        return project


    def delete(self, apiclient, **kwargs):
        cmd = deleteProject.deleteProjectCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        project = apiclient.deleteProject(cmd)
        return project


    def delete_account(self, apiclient, projectid, **kwargs):
        cmd = deleteAccountFromProject.deleteAccountFromProjectCmd()
        cmd.id = self.id
        cmd.projectid = projectid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        deletefromproject = apiclient.deleteAccountFromProject(cmd)
        return deletefromproject


    def add_account(self, apiclient, projectid, **kwargs):
        cmd = addAccountToProject.addAccountToProjectCmd()
        cmd.id = self.id
        cmd.projectid = projectid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        accounttoproject = apiclient.addAccountToProject(cmd)
        return accounttoproject

    @classmethod
    def list_accounts(self, apiclient, projectid, **kwargs):
        cmd = listProjectAccounts.listProjectAccountsCmd()
        cmd.projectid = projectid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        projectaccounts = apiclient.listProjectAccounts(cmd)
        return map(lambda e: Project(e.__dict__), projectaccounts)
