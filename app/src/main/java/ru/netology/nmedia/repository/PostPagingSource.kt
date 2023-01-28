package ru.netology.nmedia.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import retrofit2.HttpException
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.error.ApiError
import java.io.IOException

//класс для пагинации
class PostPagingSource (
    private val service: ApiService,
) : PagingSource<Long, Post>() {
    //использование определенного ключа при обновлении данных
    override fun getRefreshKey(state: PagingState<Long, Post>): Long? {
        return null
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        try {
            //тип response - Response<List<Post>>
            val response = when (params) {
                //при обновлении данных запрос на бэкенд для получения свежих постов
                is LoadParams.Refresh -> service.getLatest(params.loadSize)
                /*когда скроллит вверх. Если возвращаем объект с такими данными,
                то данное событие больше не поступит в ф-цию Load(когда скроллит вверх не будет загружаться
                новая страница*/
                is LoadParams.Prepend -> return LoadResult.Page(
                    data = emptyList(),
                    prevKey = params.key,
                    nextKey = null
                )
                //скроллит вниз
                is LoadParams.Append -> service.getBefore(params.key, params.loadSize)
            }
            //если ответ неудачный
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            val body = response.body() ?: throw ApiError(
                response.code(),
                response.message(),
            )
            val data = response.body().orEmpty()

            return LoadResult.Page(
                data = data,
                prevKey = params.key,
                nextKey = data.lastOrNull()?.id,
            )
        } catch (e: IOException) {
            return LoadResult.Error(e)
        }
    }
}