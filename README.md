# Book Recommender

## TL;DR

Item-based collaborative filtering (CF) to predict book ratings; reasonably accurate predictions (+/- 1.65 error on a rating scale of 1-10) if the user has rated at least 2 items. However, this improvement in accuracy comes at the cost of making fewer predictions.

## Goals

Given a book and a user who has not rated this book, predict the user’s rating for the book. The predicted ratings can then be used to rank book recommendations.

## Data

Collected by Cai-Nicolas Ziegler in a 4-week crawl (August / September 2004) from the Book-Crossing community. Contains 278,858 users (anonymized but with demographic information) providing 1,149,780 ratings (explicit / implicit) about 271,379 books. Ratings are either explicit, expressed on a scale from 1-10 (higher values denoting higher appreciation), or implicit, expressed by 0.

## Preprocessing

Implicit ratings are removed; only explicit ratings are retained. One duplicate rating was also removed. After preprocessing, input size is 433,670 ratings.

## Method

I implemented item-based CF because it is more scalable than user-based CF. Unlike user-based CF, the similarity matrix can be computed ahead of time because comparisons between items will not change as often as comparisons between users. Also, a site that sells millions of items may have very little overlap between users, which makes it difficult to decide which users are similar.

After preprocessing, I split the ratings into k folds so they can be used for k-fold validation.

The training set is then put into a hash table where the ratings can be looked up by user id and book id (ISBN). The rating table is used to build the similarity matrix, where each item is compared to every other item to generate the similarity measure for the item pair. I chose cosine similarity as the similarity measure because it is simple and fast (Ekstrand et al, 2011).

The similarity matrix is another hash table where the similarity score can be looked up by a unique composite key formed by the item pair (sort the two book ids and concatenate them).  

After collecting a set S of items similar to i, p(u,i) can be predicted as follows:

![](formula.jpg?raw=true)

Unlike the prediction formula in Ekstrand et al (2010), I did not implement an upper bound on the size of S. This is because the rating matrix is sparse, i.e. there are few ratings per user. This is also known as the cold start problem.

Instead, I propose a minimum size m for S, with the aim to improve accuracy at the cost of making fewer predictions.

The predicted rating is round to the nearest integer. Round half up, because according to the CEO of TripAdvisor, people tend to give [more positive reviews](https://www.linkedin.com/pulse/force-good-how-tripadvisor-changed-way-we-travel-steve-kaufer) than negative ones.

 

## Results

Let k be the number of folds and m be the minimum size of neighbourhood S of similar items.

MAE - Mean Absolute Error, average of k folds (lower is better)

RMSE - Root Mean Squared Error, average of k folds (lower is better)

Cumulative #predictions made across all folds

Prediction cannot be made for some test entries because the similarity measure is not available for the item pair, or user has insufficient number of ratings.

Below are the results of each run, adjusting k or m:

<table>
  <tr>
    <td>k, #folds</td>
    <td>m, min#ratings</td>
    <td>Avg. MAE</td>
    <td>Avg. RMSE</td>
    <td>#Predictions</td>
  </tr>
  <tr>
    <td>2</td>
    <td>1</td>
    <td>5.16</td>
    <td>5.92</td>
    <td>370K</td>
  </tr>
  <tr>
    <td>2</td>
    <td>3</td>
    <td>1.12</td>
    <td>1.59</td>
    <td>46K</td>
  </tr>
  <tr>
    <td>2</td>
    <td>2</td>
    <td>1.15</td>
    <td>1.65</td>
    <td>62K</td>
  </tr>
  <tr>
    <td>3</td>
    <td>2</td>
    <td>1.16</td>
    <td>1.65</td>
    <td>93K</td>
  </tr>
</table>


I found that accuracy is improved if m is increased, at the cost of making fewer predictions. The average RMSE drops dramatically from 5.92 to 1.65, just by increasing m from 1 to 2. Operationally, this means that **reasonably accurate predictions can be made if the user has rated at least 2 items.** 

Attempted k=4 but did not complete evaluation because a single test takes about 8 hours on a single machine (my home desktop). Computing the similarity matrix takes the most time, there were almost 50 million entries in the similarity matrix at k=4. As the number of folds increase, the training set grows and so does the size of the similarity matrix.

## Future Work

* Incorporate the use of implicit ratings, user demographics

* Hybrid system mixing content-based recommender and item-based collaborative filtering

* Speed up the computation of the similarity matrix

* Online interactive recommender

## Source Code

Written in Java, the source code can be found [here](../src/main/java/toy/bx).

## References

1. [Improving Recommendation Lists Through Topic Diversification, Cai-Nicolas Ziegler, Sean M. McNee, Joseph A. Konstan, Georg Lausen; Proceedings of the 14th International World Wide Web Conference (WWW '05), May 10-14, 2005, Chiba, Japan. To appear.](http://www2.informatik.uni-freiburg.de/~cziegler/BX/) 

2. [Michael D. Ekstrand, John T. Riedl, and Joseph A. Konstan. 2011. Collaborative Filtering Recommender Systems. Found. Trends Hum.-Comput. Interact. 4, 2 (February 2011), 81-173.](http://herbrete.vvv.enseirb-matmeca.fr/IR/CF_Recsys_Survey.pdf) 

3. Toby Segaran. 2007. Programming Collective Intelligence (First ed.). O'Reilly.

