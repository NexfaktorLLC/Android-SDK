/*
 * ********************************************************************************************************************
 *  <p/>
 *  BACKENDLESS.COM CONFIDENTIAL
 *  <p/>
 *  ********************************************************************************************************************
 *  <p/>
 *  Copyright 2012 BACKENDLESS.COM. All Rights Reserved.
 *  <p/>
 *  NOTICE: All information contained herein is, and remains the property of Backendless.com and its suppliers,
 *  if any. The intellectual and technical concepts contained herein are proprietary to Backendless.com and its
 *  suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by trade secret
 *  or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden
 *  unless prior written permission is obtained from Backendless.com.
 *  <p/>
 *  ********************************************************************************************************************
 */

package com.backendless;

import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessException;
import com.backendless.geo.BackendlessGeoQuery;
import com.backendless.persistence.BackendlessDataQuery;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unchecked")
public class BackendlessCollection<E>
{
  private final Object dataLock = new Object();
  private Class<E> type;
  private List<E> data = new CopyOnWriteArrayList<E>();
  private int totalObjects;
  private volatile IBackendlessQuery query;

  public BackendlessCollection()
  {
  }

  public Class<E> getType()
  {
    return type;
  }

  public void setType( Class<E> type )
  {
    this.type = type;
  }

  public void setPageSize( int pageSize )
  {
    query.setPageSize( pageSize );
  }

  public int getTotalObjects()
  {
    return totalObjects;
  }

  public void setTotalObjects( int totalObjects )
  {
    this.totalObjects = totalObjects;
  }

  public List<E> getCurrentPage()
  {
    return data;
  }

  //Sync methods
  public BackendlessCollection<E> nextPage() throws BackendlessException
  {
    int offset = query.getOffset();
    int pageSize = query.getPageSize();

    return getPage( pageSize, offset + pageSize );
  }

  public BackendlessCollection<E> getPage( int pageSize, int offset ) throws BackendlessException
  {
    return downloadPage( pageSize, offset );
  }

  //Download page logic
  private BackendlessCollection<E> downloadPage( int pageSize, int offset ) throws BackendlessException
  {
    IBackendlessQuery tempQuery = query.newInstance();
    tempQuery.setOffset( offset );
    tempQuery.setPageSize( pageSize );

    if( tempQuery instanceof BackendlessGeoQuery )
    {
      return (BackendlessCollection<E>) Backendless.Geo.getPoints( (BackendlessGeoQuery) tempQuery );
    }
    else
    {
      if( type == null )
      {
        throw new BackendlessException( "Collection is empty" );
      }
      else
      {
        return Backendless.Persistence.find( type, (BackendlessDataQuery) tempQuery );
      }
    }
  }

  public BackendlessCollection<E> previousPage() throws BackendlessException
  {
    int offset = query.getOffset();
    int pageSize = query.getPageSize();

    if( (offset - pageSize) >= 0 )
    {
      return getPage( pageSize, offset - pageSize );
    }
    else
    {
      return this.newInstance();
    }
  }

  protected BackendlessCollection<E> newInstance()
  {
    BackendlessCollection<E> result = new BackendlessCollection<E>();
    result.setData( data );
    result.setQuery( query );
    result.setType( type );
    result.setTotalObjects( totalObjects );

    return result;
  }

  <E> BackendlessCollection<E> newInstance( List<E> newData )
  {
    BackendlessCollection<E> result = new BackendlessCollection<E>();
    result.setData( newData );
    result.setQuery( query );

    if( !newData.isEmpty() )
    {
      result.setType( (Class<E>) newData.get( 0 ).getClass() );
    }

    result.setTotalObjects( totalObjects );

    return result;
  }

  public void setData( List<E> data )
  {
    synchronized( dataLock )
    {
      this.data = new CopyOnWriteArrayList<E>( data );
    }
  }

  public void setQuery( IBackendlessQuery query )
  {
    if( query == null )
    {
      this.query = null;
    }
    else
    {
      this.query = query.newInstance();
    }
  }

  //Async methods
  public void nextPage( AsyncCallback<BackendlessCollection<E>> responder )
  {
    new CollectionDownloadTask( responder )
    {
      @Override
      protected BackendlessCollection<E> doInBackground( int pageSize, int offset ) throws Exception
      {
        return nextPage();
      }
    }.executeThis();
  }

  public void previousPage( AsyncCallback<BackendlessCollection<E>> responder )
  {
    new CollectionDownloadTask( responder )
    {
      @Override
      protected BackendlessCollection<E> doInBackground( int pageSize, int offset ) throws Exception
      {
        return previousPage();
      }
    }.executeThis();
  }

  public void getPage( int pageSize, int offset, AsyncCallback<BackendlessCollection<E>> responder )
  {
    new CollectionDownloadTask( responder )
    {
      @Override
      protected BackendlessCollection<E> doInBackground( int pageSize, int offset ) throws Exception
      {
        return getPage( pageSize, offset );
      }
    }.executeThis( pageSize, offset );
  }
}