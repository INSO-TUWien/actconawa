/**
 * OpenAPI definition
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v0
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { PageableObject } from './pageableObject';
import { GitBranchTrackingStatusDto } from './gitBranchTrackingStatusDto';
import { SortObject } from './sortObject';

export interface PageGitBranchTrackingStatusDto {
  totalElements?: number;
  totalPages?: number;
  first?: boolean;
  last?: boolean;
  size?: number;
  content?: Array<GitBranchTrackingStatusDto>;
  number?: number;
  sort?: SortObject;
  numberOfElements?: number;
  pageable?: PageableObject;
  empty?: boolean;
}
