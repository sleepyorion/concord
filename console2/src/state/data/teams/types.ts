/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import { Action } from 'redux';
import { ConcordId, ConcordKey, GenericOperationResult, RequestError } from '../../../api/common';

import {
    NewTeamEntry,
    NewTeamLdapGroupEntry,
    NewTeamUserEntry,
    TeamEntry,
    TeamLdapGroupEntry,
    TeamUserEntry
} from '../../../api/org/team';
import { CollectionById, RequestState } from '../common';

export interface GetTeamRequest extends Action {
    orgName: ConcordKey;
    teamName: ConcordKey;
}

export interface ListTeamsRequest extends Action {
    orgName: ConcordKey;
}

export interface CreateTeamRequest extends Action {
    orgName: ConcordKey;
    entry: NewTeamEntry;
}

export interface TeamDataResponse extends Action {
    error?: RequestError;
    items?: TeamEntry[];
}

export interface RenameTeamRequest extends Action {
    orgName: ConcordKey;
    teamId: ConcordId;
    teamName: ConcordKey;
}

export interface DeleteTeamRequest extends Action {
    orgName: ConcordKey;
    teamName: ConcordKey;
}

export interface ListTeamUsersRequest extends Action {
    orgName: ConcordKey;
    teamName: ConcordKey;
}

export interface ListTeamUsersResponse extends Action {
    error?: RequestError;
    items?: TeamUserEntry[];
}

export interface ListTeamLdapGroupsRequest extends Action {
    orgName: ConcordKey;
    teamName: ConcordKey;
}

export interface ListTeamLdapGroupsResponse extends Action {
    error?: RequestError;
    items?: TeamLdapGroupEntry[];
}

export interface ReplaceTeamUsersRequest extends Action {
    orgName: ConcordKey;
    teamName: ConcordKey;
    users: NewTeamUserEntry[];
}

export interface ReplaceTeamLdapGroupsRequest extends Action {
    orgName: ConcordKey;
    teamName: ConcordKey;
    groups: NewTeamLdapGroupEntry[];
}

export type GetTeamState = RequestState<TeamDataResponse>;
export type ListTeamsState = RequestState<TeamDataResponse>;
export type CreateOrUpdateTeamState = RequestState<{}>;
export type RenameTeamState = RequestState<{}>;
export type DeleteTeamState = RequestState<GenericOperationResult>;
export type ListTeamUsersState = RequestState<ListTeamUsersResponse>;
export type ReplaceTeamUsersState = RequestState<{}>;
export type ListTeamLdapGroupsState = RequestState<ListTeamLdapGroupsResponse>;
export type ReplaceTeamLdapGroupsState = RequestState<{}>;

export type Teams = CollectionById<TeamEntry>;

export interface State {
    teamById: Teams;

    get: GetTeamState;
    list: ListTeamsState;
    create: CreateOrUpdateTeamState;
    rename: RenameTeamState;
    deleteTeam: DeleteTeamState;

    listUsers: ListTeamUsersState;
    listLdapGroups: ListTeamLdapGroupsState;
    replaceUsers: ReplaceTeamUsersState;
    replaceLdapGroups: ReplaceTeamLdapGroupsState;
}
