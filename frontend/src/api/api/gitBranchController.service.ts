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
/* tslint:disable:no-unused-variable member-ordering */

import { Inject, Injectable, Optional } from '@angular/core';
import {
  HttpClient,
  HttpContext,
  HttpEvent,
  HttpHeaders,
  HttpParameterCodec,
  HttpParams,
  HttpResponse
} from '@angular/common/http';
import { CustomHttpParameterCodec } from '../encoder';
import { Observable } from 'rxjs';

// @ts-ignore
import { PageGitBranchDto } from '../model/pageGitBranchDto';
// @ts-ignore
import { Pageable } from '../model/pageable';

// @ts-ignore
import { BASE_PATH, COLLECTION_FORMATS } from '../variables';
import { Configuration } from '../configuration';

@Injectable({
  providedIn: 'root'
})
export class GitBranchControllerService {

  protected basePath = 'http://localhost:8080';

  public defaultHeaders = new HttpHeaders();

  public configuration = new Configuration();

  public encoder: HttpParameterCodec;

  constructor(protected httpClient: HttpClient, @Optional() @Inject(BASE_PATH) basePath: string | string[], @Optional() configuration: Configuration) {
    if (configuration) {
      this.configuration = configuration;
    }
    if (typeof this.configuration.basePath !== 'string') {
      if (Array.isArray(basePath) && basePath.length > 0) {
        basePath = basePath[ 0 ];
      }

      if (typeof basePath !== 'string') {
        basePath = this.basePath;
      }
      this.configuration.basePath = basePath;
    }
    this.encoder = this.configuration.encoder || new CustomHttpParameterCodec();
  }

  // @ts-ignore
  private addToHttpParams(httpParams: HttpParams, value: any, key?: string): HttpParams {
    if (typeof value === "object" && value instanceof Date === false) {
      httpParams = this.addToHttpParamsRecursive(httpParams, value);
    } else {
      httpParams = this.addToHttpParamsRecursive(httpParams, value, key);
    }
    return httpParams;
  }

  private addToHttpParamsRecursive(httpParams: HttpParams, value?: any, key?: string): HttpParams {
    if (value == null) {
      return httpParams;
    }

    if (typeof value === "object") {
      if (Array.isArray(value)) {
        (value as any[]).forEach(elem => httpParams = this.addToHttpParamsRecursive(httpParams, elem, key));
      } else if (value instanceof Date) {
        if (key != null) {
          httpParams = httpParams.append(key, (value as Date).toISOString().substr(0, 10));
        } else {
          throw Error("key may not be null if value is Date");
        }
      } else {
        Object.keys(value).forEach(k => httpParams = this.addToHttpParamsRecursive(
                httpParams, value[ k ], key != null ? `${key}.${k}` : k));
      }
    } else if (key != null) {
      httpParams = httpParams.append(key, value);
    } else {
      throw Error("key may not be null if value is not object or array");
    }
    return httpParams;
  }

  /**
   * @param pageable
   * @param observe set whether or not to return the data Observable as the body, response or events. defaults to
   *         returning the body.
   * @param reportProgress flag to report request and response progress.
   */
  public findAllBranches(pageable: Pageable, observe?: 'body', reportProgress?: boolean, options?: {
    httpHeaderAccept?: 'application/json',
    context?: HttpContext
  }): Observable<PageGitBranchDto>;
  public findAllBranches(pageable: Pageable, observe?: 'response', reportProgress?: boolean, options?: {
    httpHeaderAccept?: 'application/json',
    context?: HttpContext
  }): Observable<HttpResponse<PageGitBranchDto>>;
  public findAllBranches(pageable: Pageable, observe?: 'events', reportProgress?: boolean, options?: {
    httpHeaderAccept?: 'application/json',
    context?: HttpContext
  }): Observable<HttpEvent<PageGitBranchDto>>;
  public findAllBranches(pageable: Pageable, observe: any = 'body', reportProgress: boolean = false, options?: {
    httpHeaderAccept?: 'application/json',
    context?: HttpContext
  }): Observable<any> {
    if (pageable === null || pageable === undefined) {
      throw new Error('Required parameter pageable was null or undefined when calling findAllBranches.');
    }

    let localVarQueryParameters = new HttpParams({encoder: this.encoder});
    if (pageable !== undefined && pageable !== null) {
      localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
              <any>pageable, 'pageable');
    }

    let localVarHeaders = this.defaultHeaders;

    let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
    if (localVarHttpHeaderAcceptSelected === undefined) {
      // to determine the Accept header
      const httpHeaderAccepts: string[] = [
        'application/json'
      ];
      localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
    }
    if (localVarHttpHeaderAcceptSelected !== undefined) {
      localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
    }

    let localVarHttpContext: HttpContext | undefined = options && options.context;
    if (localVarHttpContext === undefined) {
      localVarHttpContext = new HttpContext();
    }

    let responseType_: 'text' | 'json' | 'blob' = 'json';
    if (localVarHttpHeaderAcceptSelected) {
      if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
        responseType_ = 'text';
      } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
        responseType_ = 'json';
      } else {
        responseType_ = 'blob';
      }
    }

    let localVarPath = `/branches`;
    return this.httpClient.request<PageGitBranchDto>('get', `${this.configuration.basePath}${localVarPath}`,
            {
              context: localVarHttpContext,
              params: localVarQueryParameters,
              responseType: <any>responseType_,
              withCredentials: this.configuration.withCredentials,
              headers: localVarHeaders,
              observe: observe,
              reportProgress: reportProgress
            }
    );
  }

}
